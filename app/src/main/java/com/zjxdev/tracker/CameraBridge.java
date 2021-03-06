package com.zjxdev.tracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraBridge {
    private static final String TAG = CameraBridge.class.getSimpleName();
    private static final int MAX_PREVIEW_WIDTH = 1280;
    private static final int MAX_PREVIEW_HEIGHT = 720;

    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private Size mPreviewSize;

    private Context mContext;
    private AutoFitTextureView mTextureView;
    private Surface mDisplaySurface;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mStartFlag = false;

    private boolean mBackFacing = false;
    private int mFps = 30;
    private boolean mSwapDimensions = false;

    private RenderScript mRS;
    private Yuv2RgbaConversion mConversion;

    private static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Use render script to do YUV to RGBA conversion.
     */
    private static class Yuv2RgbaConversion implements Allocation.OnBufferAvailableListener {
        private Allocation mInputAllocation;
        private Allocation mOutputAllocation;
        private Allocation mTempInAllocation;
        private Allocation mTempOutAllocation;
        private ScriptC_yuv2rgba mScriptC;

        private Size mSize;
        private byte[] mOutputBuffer;
        private CameraBridge mFrameCallback;
        private boolean mSwap;
        private boolean mFlip;
        private boolean processing = false;

        Yuv2RgbaConversion(RenderScript rs, Size dimensions,
                           CameraBridge frameCallback, boolean swap, boolean flip) {
            mSize = dimensions;
            mFrameCallback = frameCallback;
            mSwap = swap;
            mFlip = flip;
            createAllocations(rs);
            mInputAllocation.setOnBufferAvailableListener(this);

            mScriptC = new ScriptC_yuv2rgba(rs);
            mScriptC.set_gInputFrame(mInputAllocation);
            mScriptC.set_gOuputFrame(mOutputAllocation);
            mScriptC.set_gWidth(mSize.getWidth());
            mScriptC.set_gHeight(mSize.getHeight());
        }

        private void createAllocations(RenderScript rs) {
            mOutputBuffer = new byte[mSize.getWidth() * mSize.getHeight() * 4];

            final int width = mSize.getWidth();
            final int height = mSize.getHeight();

            Type.Builder yuvTypeBuilder = new Type.Builder(rs, Element.YUV(rs));
            yuvTypeBuilder.setX(width);
            yuvTypeBuilder.setY(height);
            yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888);
            mInputAllocation = Allocation.createTyped(rs, yuvTypeBuilder.create(),
                    Allocation.USAGE_IO_INPUT | Allocation.USAGE_SCRIPT);

            Type rgbaType = Type.createXY(rs, Element.U8_4(rs), width, height);
            mTempInAllocation = Allocation.createTyped(rs, rgbaType,
                    Allocation.USAGE_SCRIPT);
            mTempOutAllocation = Allocation.createTyped(rs, rgbaType,
                    Allocation.USAGE_SCRIPT);

            if(mSwap) {
                Type rgbaSwapType = Type.createXY(rs, Element.U8_4(rs), height, width);
                mOutputAllocation = Allocation.createTyped(rs, rgbaSwapType,
                        Allocation.USAGE_SCRIPT);
            } else {
                mOutputAllocation = Allocation.createTyped(rs, rgbaType,
                        Allocation.USAGE_SCRIPT);
            }
        }

        Surface getInputSurface() {
            return mInputAllocation.getSurface();
        }

        @Override
        public void onBufferAvailable(Allocation a) {
            if(processing) return;
            processing = true;

            // Get the new frame into the input allocation
            mInputAllocation.ioReceive();
            // Run processing pass if we should send a frame
            if(mSwap && mFlip) {
                mScriptC.forEach_yuv2rgba_swapflip(mTempInAllocation, mTempOutAllocation);
            } else if(mSwap) {
                mScriptC.forEach_yuv2rgba_swap(mTempInAllocation, mTempOutAllocation);
            } else if(mFlip) {
                mScriptC.forEach_yuv2rgba_flip(mTempInAllocation, mTempOutAllocation);
            } else {
                mScriptC.forEach_yuv2rgba(mTempInAllocation, mTempOutAllocation);
            }

            if (mFrameCallback != null) {
                mOutputAllocation.copyTo(mOutputBuffer);
                mFrameCallback.onFrameArray(mOutputBuffer);
            }

            processing = false;
        }
    }

    public CameraBridge(AutoFitTextureView texture, Context context, boolean useBackCamera, int fps) {
        mTextureView = texture;
        mContext = context;
        mBackFacing = useBackCamera;
        mFps = fps;
        mRS = RenderScript.create(mContext);
    }

    /**
     * TextureView should be recreated when recover from background.
     */
    public void setTextureView(AutoFitTextureView texture) {
        mTextureView = texture;
    }

    public void onResume() {
        if(mStartFlag) return;
        mStartFlag = true;
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void onPause() {
        if(!mStartFlag) return;
        mStartFlag = false;
        stopBackgroundThread();
        closeCamera();
    }

    public void onDestroy() {
        if(!mStartFlag) return;
        mStartFlag = false;
        stopBackgroundThread();
        closeCamera();
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight) {
        List<Size> allowed = new ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight) {
                allowed.add(option);
            }
        }

        double targetRatio = (double) textureViewWidth / textureViewHeight;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        double tempDiff = 0;
        int targetHeight = textureViewHeight;

        // Try to find an size match aspect ratio and size
        for (Size size : allowed) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) <= 0.1) {
                tempDiff = Math.abs(size.getHeight() - targetHeight);
                if (tempDiff < minDiff) {
                    optimalSize = size;
                    minDiff = tempDiff;
                }
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : allowed) {
                tempDiff = Math.abs(size.getHeight() - targetHeight);
                if (tempDiff < minDiff) {
                    optimalSize = size;
                    minDiff = tempDiff;
                }
            }
        }

        return optimalSize;
    }

    private static Range<Integer> chooseOptimalFps(Range<Integer>[] choices, int target) {
        Range<Integer> bestFramerate = new Range<Integer>(0, 0);
        int delta = Integer.MAX_VALUE;
        int temp = 0;
        for (Range<Integer> option : choices) {
            temp = Math.abs(option.getUpper() - target);
            if (temp < delta) {
                bestFramerate = option;
                delta = temp;
            } else if (temp == delta) {
                bestFramerate = bestFramerate.getLower() < option.getLower() ? option : bestFramerate;
            }
        }
        return bestFramerate;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != (mBackFacing ? CameraCharacteristics.LENS_FACING_BACK : CameraCharacteristics.LENS_FACING_FRONT)) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // Find out if we need to swap dimension to get the preview size relative to sensor coordinate.
                WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                int displayRotation = wm.getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                int mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                mSwapDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            mSwapDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            mSwapDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                wm.getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (mSwapDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(Allocation.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                if(mSwapDimensions) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }

                mConversion = new Yuv2RgbaConversion(mRS, mPreviewSize, this, mSwapDimensions, !mBackFacing);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.
            Log.e(TAG, "Camera2 API not supported.");
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            if (mSwapDimensions) {
                texture.setDefaultBufferSize(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            } else {
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }

            // This is the output Surface we need to start preview.
            mDisplaySurface = new Surface(texture);
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface inputSurface = mConversion.getInputSurface();
            mPreviewRequestBuilder.addTarget(inputSurface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Collections.singletonList(inputSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON);
                                CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
                                Range<Integer> fps[] = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, chooseOptimalFps(fps, mFps));

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "Capture session configuration failed.");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize || null == mContext) {
            return;
        }
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(4000, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Do custom per frame process and display.
     */
    public void onFrameArray(byte[] frameData) {
        if(mSwapDimensions) {
            JNIDisplay.SimpleRGBADisplay(mPreviewSize.getHeight(), mPreviewSize.getWidth(), frameData, mDisplaySurface);
        } else {
            JNIDisplay.SimpleRGBADisplay(mPreviewSize.getWidth(), mPreviewSize.getHeight(), frameData, mDisplaySurface);
        }
    }

}
