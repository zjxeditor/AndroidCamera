package com.zjxdev.tracker;

import android.media.Image;
import android.view.Surface;

import java.nio.ByteBuffer;

public class JNIDisplay {
    private static final String TAG = JNIDisplay.class.getSimpleName();

    static {
        System.loadLibrary("nativedisplay");
    }

    public static native void RGBADisplay(int srcWidth, int srcHeight,
                                          int Y_rowStride, ByteBuffer Y_Buffer,
                                          int UV_rowStride, ByteBuffer U_Buffer,
                                          ByteBuffer V_Buffer, Surface surface, boolean swapDim);
}
