package com.zjxdev.tracker;

import android.util.Log;

public class CSRT {
    private static boolean libLoadFlag = false;
    static {
        if(!libLoadFlag) {
            libLoadFlag = true;
            System.loadLibrary("nativetracker");
        }
    }

    enum WindowType {
        Hann(0), Cheb(1), Kaiser(2);
        private int value;
        WindowType(int value) {
            this.value = value;
        }
        int Value() {
            return this.value;
        }
    }

    static class TrackerParams {
        // Feature related parameters.
        boolean UseHOG = true;
        boolean UseCN = true;
        boolean UseGRAY = true;
        boolean UseRGB = false;
        int NumHOGChannelsUsed = 18;
        boolean UsePCA = false;                     // PCA features.
        int PCACount = 8;                           // Feature PCA count.
        //float HogOrientations;
        //float HogClip;

        boolean UseChannelWeights = true;           // Use different weights for each channel.
        boolean UseSegmentation = true;             // Use segmentation for spatial constraint.

        int AdmmIterations = 4;                     // Iteration number for optimized filter solver.
        float Padding = 3.0f;                       // Padding used to calculate template size. Affect how much area to search.
        int TemplateSize = 200;                     // Specify the target template size.
        float GaussianSigma = 1.0f;                 // Guassian lable sigma factor.

        int WindowFunc = WindowType.Hann.Value();   // Filter window function type.
        float ChebAttenuation = 45.0f;              // Attenuation when use Cheb window.
        float KaiserAlpha = 3.75f;                  // Alpha value when use Kaiser window.

        float WeightsLearnRate = 0.02f;             // Filter weights learn rate.
        float FilterLearnRate = 0.02f;              // Filter learn rate.
        float HistLearnRate = 0.04f;                // Histogram model learn rate.
        float ScaleLearnRate = 0.025f;              // DSST learn rate.

        float BackgroundRatio = 2.0f;               // Background extend ratio.
        int HistogramBins = 16;                     // Bins number for the hisogram extraction.
        int PostRegularCount = 10;                  // Iteration count for post regularization count in segment process.
        float MaxSegmentArea = 1024.0f;             // Controls the max area used for segment probability computation.

        int ScaleCount = 33;                        // Scale numbers for DSST.
        float ScaleSigma = 0.25f;                   // DSST scale sigma factor.
        float ScaleMaxArea = 512.0f;                // Max model area for DSST.
        float ScaleStep = 1.02f;                    // Scale step for DSST.

        int UpdateInterval = 4;                     // Update frame interval. Set to 0 or negative to disable background update mode.
        float PeakRatio = 0.1f;                     // Specify the peak occupation ratio to calculate PSR. PeakSize = ResponseSize * PeakRatio * 2 + 1.
        float PSRThreshold = 15.0f;                 // PSR threshold used for failure detection. 20.0 ~ 60.0 is indicates strong peaks.

        // Fetch jni filed ids only once.
        private native void InitJniFieldIDs();
        static{
            if(!libLoadFlag) {
                libLoadFlag = true;
                System.loadLibrary("nativetracker");
            }
            new TrackerParams().InitJniFieldIDs();
        }
    }

    static class Bounds {
        Bounds() {
        }
        Bounds(int xx0, int yy0, int w, int h) {
            x0 = xx0;
            y0 = yy0;
            x1 = xx0 + w;
            y1 = yy0 + h;
        }

        int x0 = 0;
        int y0 = 0;
        int x1 = 0;
        int y1 = 0;

        // Fetch jni filed ids only once.
        private native void InitJniFieldIDs();
        static{
            if(!libLoadFlag) {
                libLoadFlag = true;
                System.loadLibrary("nativetracker");
            }
            new Bounds().InitJniFieldIDs();
        }
    }

    static class TrackerResult {
        boolean Succeed = false;
        float Score = 0.0f;
        Bounds Box = new Bounds();

        TrackerResult() {}

        // Fetch jni filed ids only once.
        private native void InitJniFieldIDs();
        static{
            if(!libLoadFlag) {
                libLoadFlag = true;
                System.loadLibrary("nativetracker");
            }
            new TrackerResult().InitJniFieldIDs();
        }
    }

    CSRT(int rows, int cols) {
        this(rows, cols, new TrackerParams(), "");
    }

    CSRT(int rows, int cols, TrackerParams params) {
        this(rows, cols, params, "");
    }

    CSRT(int rows, int cols, String logPath) {
        this(rows, cols, new TrackerParams(), logPath);
    }

    CSRT(int rows, int cols, TrackerParams params, String logPath) {
        if(!singleFlag)
            singleFlag = true;
        else {
            Log.e("CSRT", "Only one instance of tracker can exist.");
            return;
        }
        StartSystem(logPath);
        trackerPtr = CreateTracker(rows, cols, params);
    }

    void dispose() {
        if(trackerPtr == 0) return;
        DeleteTracker(trackerPtr);
        trackerPtr = 0;
        singleFlag = false;
        CloseSystem();
    }
    protected void finalize() { dispose(); }

    void Initialize(long dataPtr, Bounds bb) {
        if(trackerPtr == 0) return;
        InitializeTracker(dataPtr, bb, trackerPtr);
    }

    TrackerResult Update(long dataPtr) {
        if(trackerPtr == 0) return new TrackerResult();
        TrackerResult res = new TrackerResult();
        UpdateTracker(dataPtr, res, trackerPtr);
        return res;
    }

    void SetReinitialzie() {
        if(trackerPtr == 0) return;
        ReinitialzieTracker(trackerPtr);
    }

    // Hold the pointer to native tracker class.
    private long trackerPtr = 0;
    // Flag for singleton.
    private static boolean singleFlag = false;

    // Native methods.
    private native void StartSystem(String path);
    private native void CloseSystem();
    private native long CreateTracker(int rows, int cols, TrackerParams params);
    private native void DeleteTracker(long trackerPtr);
    private native void InitializeTracker(long dataPtr, Bounds bb, long trackerPtr);
    private native void UpdateTracker(long dataPtr, TrackerResult res, long trackerPtr);
    private native void ReinitialzieTracker(long tracker);
}