package com.zjxdev.tracker;

import android.util.Log;

/**
 * The Java interface for CSRT tracker. Only 1 tracker can be created. Not support for multi object tracking.
 */
public class CSRT {
    private static final String TAG = CSRT.class.getSimpleName();
    private static boolean libLoadFlag = false;     // Flag for library loading.

    static {
        if (!libLoadFlag) {
            libLoadFlag = true;
            System.loadLibrary("nativetracker");
        }
    }

    /**
     * Filter window type.
     */
    public enum WindowType {
        Hann(0), Cheb(1), Kaiser(2);
        private int value;

        WindowType(int value) {
            this.value = value;
        }

        public int Value() {
            return this.value;
        }
    }

    /**
     * CSRT tracker parameters.
     */
    public static class TrackerParams {
        // Feature related parameters.
        public boolean UseHOG = true;
        public boolean UseCN = true;
        public boolean UseGRAY = true;
        public boolean UseRGB = false;
        public int NumHOGChannelsUsed = 18;
        public boolean UsePCA = false;                     // PCA features.
        public int PCACount = 8;                           // Feature PCA count.

        public boolean UseChannelWeights = true;           // Use different weights for each channel.
        public boolean UseSegmentation = true;             // Use segmentation for spatial constraint.

        public int AdmmIterations = 4;                     // Iteration number for optimized filter solver.
        public float Padding = 3.0f;                       // Padding used to calculate template size. Affect how much area to search.
        public int TemplateSize = 200;                     // Specify the target template size.
        public float GaussianSigma = 1.0f;                 // Guassian lable sigma factor.

        public int WindowFunc = WindowType.Hann.Value();   // Filter window function type.
        public float ChebAttenuation = 45.0f;              // Attenuation when use Cheb window.
        public float KaiserAlpha = 3.75f;                  // Alpha value when use Kaiser window.

        public float WeightsLearnRate = 0.02f;             // Filter weights learn rate.
        public float FilterLearnRate = 0.02f;              // Filter learn rate.
        public float HistLearnRate = 0.04f;                // Histogram model learn rate.
        public float ScaleLearnRate = 0.025f;              // DSST learn rate.

        public float BackgroundRatio = 2.0f;               // Background extend ratio.
        public int HistogramBins = 16;                     // Bins number for the hisogram extraction.
        public int PostRegularCount = 10;                  // Iteration count for post regularization count in segment process.
        public float MaxSegmentArea = 1024.0f;             // Controls the max area used for segment probability computation.

        public int ScaleCount = 33;                        // Scale numbers for DSST.
        public float ScaleSigma = 0.25f;                   // DSST scale sigma factor.
        public float ScaleMaxArea = 512.0f;                // Max model area for DSST.
        public float ScaleStep = 1.02f;                    // Scale step for DSST.

        public int UpdateInterval = 4;                     // Update frame interval. Set to 0 or negative to disable background update mode.
        public float PeakRatio = 0.1f;                     // Specify the peak occupation ratio to calculate PSR. PeakSize = ResponseSize * PeakRatio * 2 + 1.
        public float PSRThreshold = 15.0f;                 // PSR threshold used for failure detection. 20.0 ~ 60.0 is indicates strong peaks.

        // Fetch jni filed ids only once.
        private static native void InitJniFieldIDs();

        static {
            if (!libLoadFlag) {
                libLoadFlag = true;
                System.loadLibrary("nativetracker");
            }
            InitJniFieldIDs();
        }
    }

    /**
     * Represent a 2d rect area.
     */
    public static class Bounds {
        public Bounds() {
        }

        public Bounds(int xx0, int yy0, int w, int h) {
            x0 = xx0;
            y0 = yy0;
            x1 = xx0 + w;
            y1 = yy0 + h;
        }

        public int x0 = 0;
        public int y0 = 0;
        public int x1 = 0;
        public int y1 = 0;

        // Fetch jni filed ids only once.
        private static native void InitJniFieldIDs();

        static {
            if (!libLoadFlag) {
                libLoadFlag = true;
                System.loadLibrary("nativetracker");
            }
            InitJniFieldIDs();
        }
    }

    /**
     * Tracking result for each frame.
     */
    public static class TrackerResult {
        public boolean Succeed = false;
        public float Score = 0.0f;
        public Bounds Box = new Bounds();

        // Fetch jni filed ids only once.
        private static native void InitJniFieldIDs();

        static {
            if (!libLoadFlag) {
                libLoadFlag = true;
                System.loadLibrary("nativetracker");
            }
            InitJniFieldIDs();
        }
    }


    public CSRT(int rows, int cols) {
        this(rows, cols, new TrackerParams(), "");
    }

    public CSRT(int rows, int cols, TrackerParams params) {
        this(rows, cols, params, "");
    }

    public CSRT(int rows, int cols, String logPath) {
        this(rows, cols, new TrackerParams(), logPath);
    }

    public CSRT(int rows, int cols, TrackerParams params, String logPath) {
        if (!singleFlag)
            singleFlag = true;
        else {
            Log.e(TAG, "Only one instance of tracker can exist.");
            return;
        }
        StartSystem(logPath);
        trackerPtr = CreateTracker(rows, cols, params);
    }

    /**
     * Release the native resource, do native clean work.
     */
    public void dispose() {
        if (trackerPtr == 0) return;
        DeleteTracker(trackerPtr);
        trackerPtr = 0;
        singleFlag = false;
        CloseSystem();
    }

    protected void finalize() {
        dispose();
    }

    /**
     * Initialize the tracker with image data and target object's bounding box.
     */
    public void Initialize(long dataPtr, int channels, Bounds bb) {
        if (trackerPtr == 0) {
            Log.e(TAG, "No native tracker created.");
            return;
        }
        InitializeTracker(dataPtr, channels, bb, trackerPtr);
    }

    /**
     * Update for each frame to get the current tracking result.
     */
    public TrackerResult Update(long dataPtr, int channels) {
        if (trackerPtr == 0) {
            Log.e(TAG, "No native tracker created.");
            return new TrackerResult();
        }
        TrackerResult res = new TrackerResult();
        UpdateTracker(dataPtr, channels, res, trackerPtr);
        return res;
    }

    /**
     * Must be called before re-initialize work.
     */
    public void SetReinitialzie() {
        if (trackerPtr == 0) {
            Log.e(TAG, "No native tracker created.");
            return;
        }
        ReinitialzieTracker(trackerPtr);
    }

    public void SetDrawMode(boolean enableDraw, int red, int green, int blue, float alpha) {
        if(trackerPtr == 0) {
            Log.e(TAG, "No native tracker created.");
            return;
        }
        SetTrackerDrawMode(enableDraw, red, green, blue, alpha, trackerPtr);
    }

    private long trackerPtr = 0;    // Hold the pointer to native tracker class.
    private static boolean singleFlag = false;  // Flag for singleton.

    /**
     * Native methods.
     */

    private static native void StartSystem(String path);

    private static native void CloseSystem();

    private static native long CreateTracker(int rows, int cols, TrackerParams params);

    private static native void DeleteTracker(long trackerPtr);

    private static native void InitializeTracker(long dataPtr, int channels, Bounds bb, long trackerPtr);

    private static native void UpdateTracker(long dataPtr, int channels, TrackerResult res, long trackerPtr);

    private static native void ReinitialzieTracker(long tracker);

    private static native void SetTrackerDrawMode(boolean enableDraw, int r, int g, int b, float a, long trackerPtr);
}