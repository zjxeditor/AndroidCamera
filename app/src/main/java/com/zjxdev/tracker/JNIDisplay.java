package com.zjxdev.tracker;

import android.view.Surface;

public class JNIDisplay {
    private static final String TAG = JNIDisplay.class.getSimpleName();

    static {
        System.loadLibrary("nativedisplay");
    }

    public static native void SimpleRGBADisplay(int width, int height, byte[] data, Surface surface);
}
