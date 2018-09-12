#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "Tracker", __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL Java_com_zjxdev_tracker_JNIDisplay_SimpleRGBADisplay(
        JNIEnv *env,
        jobject obj,
        jint width,
        jint height,
        jbyteArray data,
        jobject surface) {
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(window);
    ANativeWindow_Buffer buffer;
    ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);
    if (int32_t err = ANativeWindow_lock(window, &buffer, NULL)) {
        LOGE("ANativeWindow_lock failed with error code: %d\n", err);
        ANativeWindow_release(window);
        return;
    }

    uint8_t *srcPtr = reinterpret_cast<uint8_t *>(env->GetByteArrayElements(data, nullptr));
    uint8_t *dstPtr = reinterpret_cast<uint8_t *>(buffer.bits);

    for(int y = 0; y < height; ++y) {
        memcpy(dstPtr + y * buffer.stride * 4, srcPtr + y * width * 4, (size_t)width * 4);
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
}
