#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "Tracker", __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL Java_com_zjxdev_tracker_JNIDisplay_GrayscaleDisplay(
        JNIEnv *env,
        jobject obj,
        jint srcWidth,
        jint srcHeight,
        jint rowStride,
        jobject srcBuffer,
        jobject surface,
        jboolean swapDim) {
    uint8_t *srcLumaPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(srcBuffer));
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(window);
    ANativeWindow_Buffer buffer;
    ANativeWindow_setBuffersGeometry(window, 0, 0, WINDOW_FORMAT_RGBA_8888);
    if (int32_t err = ANativeWindow_lock(window, &buffer, NULL)) {
        LOGE("ANativeWindow_lock failed with error code: %d\n", err);
        ANativeWindow_release(window);
    }

    uint8_t *outPtr = reinterpret_cast<uint8_t *>(buffer.bits);
    for (size_t y = 0; y < srcHeight; y++) {
        uint8_t *rowPtr = srcLumaPtr + y * rowStride;
        for (size_t x = 0; x < srcWidth; x++) {
            //for grayscale output, just duplicate the Y channel into R, G, B channels
            *(outPtr++) = *rowPtr; //R
            *(outPtr++) = *rowPtr; //G
            *(outPtr++) = *rowPtr; //B
            *(outPtr++) = 255; // gamma for RGBA_8888
            ++rowPtr;
        }
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
}

extern "C" JNIEXPORT void JNICALL Java_com_zjxdev_tracker_JNIDisplay_RGBADisplay(
        JNIEnv *env,
        jobject obj,
        jint srcWidth,
        jint srcHeight,
        jint Y_rowStride,
        jobject Y_Buffer,
        jint UV_rowStride,
        jobject U_Buffer,
        jobject V_Buffer,
        jobject surface,
        jboolean swapDim) {
    uint8_t *srcYPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(Y_Buffer));
    uint8_t *srcUPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(U_Buffer));
    uint8_t *srcVPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(V_Buffer));
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(window);
    ANativeWindow_Buffer buffer;
    ANativeWindow_setBuffersGeometry(window, 0, 0, WINDOW_FORMAT_RGBA_8888);
    if (int32_t err = ANativeWindow_lock(window, &buffer, NULL)) {
        LOGE("ANativeWindow_lock failed with error code: %d\n", err);
        ANativeWindow_release(window);
    }

    int rightShift = UV_rowStride == Y_rowStride ? 1 : 0;
    uint8_t *outPtr = reinterpret_cast<uint8_t *>(buffer.bits);
    if(swapDim) {
        for (size_t y = 0; y < srcHeight; y++) {
            uint8_t *Y_rowPtr = srcYPtr + y * Y_rowStride;
            uint8_t *U_rowPtr = srcUPtr + (y >> 1) * UV_rowStride;
            uint8_t *V_rowPtr = srcVPtr + (y >> 1) * UV_rowStride;
            for (size_t x = 0; x < srcWidth; x++) {
                uint8_t Y = Y_rowPtr[x];
                uint8_t U = U_rowPtr[x >> 1 << rightShift];
                uint8_t V = V_rowPtr[x >> 1 << rightShift];
                double R = Y + (V - 128) * 1.402;
                double G = Y - (U - 128) * 0.34414 - (V - 128) * 0.71414;
                double B = Y + (U - 128) * 1.772;
                size_t dx = srcHeight - 1 - y;
                size_t dy = srcWidth - 1 - x;
                uint8_t *startPtr = outPtr + dy * srcHeight * 4 + dx * 4;
                *(startPtr++) = (uint8_t) (R > 255 ? 255 : (R < 0 ? 0 : R));
                *(startPtr++) = (uint8_t) (G > 255 ? 255 : (G < 0 ? 0 : G));
                *(startPtr++) = (uint8_t) (B > 255 ? 255 : (B < 0 ? 0 : B));
                *(startPtr) = 255;
            }
        }
    } else {
        for (size_t y = 0; y < srcHeight; y++) {
            uint8_t *Y_rowPtr = srcYPtr + y * Y_rowStride;
            uint8_t *U_rowPtr = srcUPtr + (y >> 1) * UV_rowStride;
            uint8_t *V_rowPtr = srcVPtr + (y >> 1) * UV_rowStride;
            for (size_t x = 0; x < srcWidth; x++) {
                uint8_t Y = Y_rowPtr[x];
                uint8_t U = U_rowPtr[x >> 1 << rightShift];
                uint8_t V = V_rowPtr[x >> 1 << rightShift];
                double R = Y + (V - 128) * 1.402;
                double G = Y - (U - 128) * 0.34414 - (V - 128) * 0.71414;
                double B = Y + (U - 128) * 1.772;
                *(outPtr++) = (uint8_t) (R > 255 ? 255 : (R < 0 ? 0 : R));
                *(outPtr++) = (uint8_t) (G > 255 ? 255 : (G < 0 ? 0 : G));
                *(outPtr++) = (uint8_t) (B > 255 ? 255 : (B < 0 ? 0 : B));
                *(outPtr++) = 255;
            }
        }
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
}
