#include <jni.h>
#include "CSRTracker.h"

struct TrackerParamsFieldIDs {
    jfieldID UseHOG_ID = nullptr;
    jfieldID UseCN_ID = nullptr;
    jfieldID UseGRAY_ID = nullptr;
    jfieldID UseRGB_ID = nullptr;
    jfieldID NumHOGChannelsUsed_ID = nullptr;
    jfieldID UsePCA_ID = nullptr;
    jfieldID PCACount_ID = nullptr;

    jfieldID UseChannelWeights_ID = nullptr;
    jfieldID UseSegmentation_ID = nullptr;

    jfieldID AdmmIterations_ID = nullptr;
    jfieldID Padding_ID = nullptr;
    jfieldID TemplateSize_ID = nullptr;
    jfieldID GaussianSigma_ID = nullptr;

    jfieldID WindowFunc_ID = nullptr;
    jfieldID ChebAttenuation_ID = nullptr;
    jfieldID KaiserAlpha_ID = nullptr;

    jfieldID WeightsLearnRate_ID = nullptr;
    jfieldID FilterLearnRate_ID = nullptr;
    jfieldID HistLearnRate_ID = nullptr;
    jfieldID ScaleLearnRate_ID = nullptr;

    jfieldID BackgroundRatio_ID = nullptr;
    jfieldID HistogramBins_ID = nullptr;
    jfieldID PostRegularCount_ID = nullptr;
    jfieldID MaxSegmentArea_ID = nullptr;

    jfieldID ScaleCount_ID = nullptr;
    jfieldID ScaleSigma_ID = nullptr;
    jfieldID ScaleMaxArea_ID = nullptr;
    jfieldID ScaleStep_ID = nullptr;

    jfieldID UpdateInterval_ID = nullptr;
    jfieldID PeakRatio_ID = nullptr;
    jfieldID PSRThreshold_ID = nullptr;

    void Convert(JNIEnv *env, jobject params, CSRT::CSRTrackerParams &nativeParams) {
        nativeParams.UseHOG = (bool) env->GetBooleanField(params, UseHOG_ID);
        nativeParams.UseCN = (bool) env->GetBooleanField(params, UseCN_ID);
        nativeParams.UseGRAY = (bool) env->GetBooleanField(params, UseGRAY_ID);
        nativeParams.UseRGB = (bool) env->GetBooleanField(params, UseRGB_ID);
        nativeParams.NumHOGChannelsUsed = env->GetIntField(params, NumHOGChannelsUsed_ID);
        nativeParams.UsePCA = (bool) env->GetBooleanField(params, UsePCA_ID);
        nativeParams.PCACount = env->GetIntField(params, PCACount_ID);

        nativeParams.UseChannelWeights = (bool) env->GetBooleanField(params, UseChannelWeights_ID);
        nativeParams.UseSegmentation = (bool) env->GetBooleanField(params, UseSegmentation_ID);

        nativeParams.AdmmIterations = env->GetIntField(params, AdmmIterations_ID);
        nativeParams.Padding = env->GetFloatField(params, Padding_ID);
        nativeParams.TemplateSize = env->GetIntField(params, TemplateSize_ID);
        nativeParams.GaussianSigma = env->GetFloatField(params, GaussianSigma_ID);

        nativeParams.WindowFunc = (CSRT::CSRTWindowType) env->GetIntField(params, WindowFunc_ID);
        nativeParams.ChebAttenuation = env->GetFloatField(params, ChebAttenuation_ID);
        nativeParams.KaiserAlpha = env->GetFloatField(params, KaiserAlpha_ID);

        nativeParams.WeightsLearnRate = env->GetFloatField(params, WeightsLearnRate_ID);
        nativeParams.FilterLearnRate = env->GetFloatField(params, FilterLearnRate_ID);
        nativeParams.HistLearnRate = env->GetFloatField(params, HistLearnRate_ID);
        nativeParams.ScaleLearnRate = env->GetFloatField(params, ScaleLearnRate_ID);

        nativeParams.BackgroundRatio = env->GetFloatField(params, BackgroundRatio_ID);
        nativeParams.HistogramBins = env->GetIntField(params, HistogramBins_ID);
        nativeParams.PostRegularCount = env->GetIntField(params, PostRegularCount_ID);
        nativeParams.MaxSegmentArea = env->GetFloatField(params, MaxSegmentArea_ID);

        nativeParams.ScaleCount = env->GetIntField(params, ScaleCount_ID);
        nativeParams.ScaleSigma = env->GetFloatField(params, ScaleSigma_ID);
        nativeParams.ScaleMaxArea = env->GetFloatField(params, ScaleMaxArea_ID);
        nativeParams.ScaleStep = env->GetFloatField(params, ScaleStep_ID);

        nativeParams.UpdateInterval = env->GetIntField(params, UpdateInterval_ID);
        nativeParams.PeakRatio = env->GetFloatField(params, PeakRatio_ID);
        nativeParams.PSRThreshold = env->GetFloatField(params, PSRThreshold_ID);
    }
};

TrackerParamsFieldIDs g_TrackerParamsFieldIDs;

struct BoundsFieldIDs {
    jfieldID x0_ID = nullptr;
    jfieldID y0_ID = nullptr;
    jfieldID x1_ID = nullptr;
    jfieldID y1_ID = nullptr;

    void Convert(JNIEnv *env, jobject bb, CSRT::Bounds &nativebb) {
        nativebb.x0 = env->GetIntField(bb, x0_ID);
        nativebb.y0 = env->GetIntField(bb, y0_ID);
        nativebb.x1 = env->GetIntField(bb, x1_ID);
        nativebb.y1 = env->GetIntField(bb, y1_ID);
    }

    void ConvertBack(JNIEnv *env, jobject bb, CSRT::Bounds &nativebb) {
        env->SetIntField(bb, x0_ID, nativebb.x0);
        env->SetIntField(bb, y0_ID, nativebb.y0);
        env->SetIntField(bb, x1_ID, nativebb.x1);
        env->SetIntField(bb, y1_ID, nativebb.y1);
    }
};

BoundsFieldIDs g_BoundsFieldIDs;

static struct TrackerResultFieldIDs {
    jfieldID Succeed_ID = nullptr;
    jfieldID Score_ID = nullptr;
    jfieldID Box_ID = nullptr;

    void SetValues(JNIEnv *env, jobject res, bool succeed, float score, CSRT::Bounds &nativebb) {
        env->SetBooleanField(res, Succeed_ID, (jboolean) succeed);
        env->SetFloatField(res, Score_ID, score);
        jobject bb = env->GetObjectField(res, Box_ID);
        g_BoundsFieldIDs.ConvertBack(env, bb, nativebb);
    }
};

TrackerResultFieldIDs g_TrackerResultFieldIDs;

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_00024TrackerParams_InitJniFieldIDs(JNIEnv *env, jobject thiz) {
    jclass thizclass = env->GetObjectClass(thiz);
    g_TrackerParamsFieldIDs.UseHOG_ID = env->GetFieldID(thizclass, "UseHOG", "Z");
    g_TrackerParamsFieldIDs.UseCN_ID = env->GetFieldID(thizclass, "UseCN", "Z");
    g_TrackerParamsFieldIDs.UseGRAY_ID = env->GetFieldID(thizclass, "UseGRAY", "Z");
    g_TrackerParamsFieldIDs.UseRGB_ID = env->GetFieldID(thizclass, "UseRGB", "Z");
    g_TrackerParamsFieldIDs.NumHOGChannelsUsed_ID = env->GetFieldID(thizclass, "NumHOGChannelsUsed",
                                                                    "I");
    g_TrackerParamsFieldIDs.UsePCA_ID = env->GetFieldID(thizclass, "UsePCA", "Z");
    g_TrackerParamsFieldIDs.PCACount_ID = env->GetFieldID(thizclass, "PCACount", "I");

    g_TrackerParamsFieldIDs.UseChannelWeights_ID = env->GetFieldID(thizclass, "UseChannelWeights",
                                                                   "Z");
    g_TrackerParamsFieldIDs.UseSegmentation_ID = env->GetFieldID(thizclass, "UseSegmentation", "Z");

    g_TrackerParamsFieldIDs.AdmmIterations_ID = env->GetFieldID(thizclass, "AdmmIterations", "I");
    g_TrackerParamsFieldIDs.Padding_ID = env->GetFieldID(thizclass, "Padding", "F");
    g_TrackerParamsFieldIDs.TemplateSize_ID = env->GetFieldID(thizclass, "TemplateSize", "I");
    g_TrackerParamsFieldIDs.GaussianSigma_ID = env->GetFieldID(thizclass, "GaussianSigma", "F");

    g_TrackerParamsFieldIDs.WindowFunc_ID = env->GetFieldID(thizclass, "WindowFunc", "I");
    g_TrackerParamsFieldIDs.ChebAttenuation_ID = env->GetFieldID(thizclass, "ChebAttenuation", "F");
    g_TrackerParamsFieldIDs.KaiserAlpha_ID = env->GetFieldID(thizclass, "KaiserAlpha", "F");

    g_TrackerParamsFieldIDs.WeightsLearnRate_ID = env->GetFieldID(thizclass, "WeightsLearnRate",
                                                                  "F");
    g_TrackerParamsFieldIDs.FilterLearnRate_ID = env->GetFieldID(thizclass, "FilterLearnRate", "F");
    g_TrackerParamsFieldIDs.HistLearnRate_ID = env->GetFieldID(thizclass, "HistLearnRate", "F");
    g_TrackerParamsFieldIDs.ScaleLearnRate_ID = env->GetFieldID(thizclass, "ScaleLearnRate", "F");

    g_TrackerParamsFieldIDs.BackgroundRatio_ID = env->GetFieldID(thizclass, "BackgroundRatio", "F");
    g_TrackerParamsFieldIDs.HistogramBins_ID = env->GetFieldID(thizclass, "HistogramBins", "I");
    g_TrackerParamsFieldIDs.PostRegularCount_ID = env->GetFieldID(thizclass, "PostRegularCount",
                                                                  "I");
    g_TrackerParamsFieldIDs.MaxSegmentArea_ID = env->GetFieldID(thizclass, "MaxSegmentArea", "F");

    g_TrackerParamsFieldIDs.ScaleCount_ID = env->GetFieldID(thizclass, "ScaleCount", "I");
    g_TrackerParamsFieldIDs.ScaleSigma_ID = env->GetFieldID(thizclass, "ScaleSigma", "F");
    g_TrackerParamsFieldIDs.ScaleMaxArea_ID = env->GetFieldID(thizclass, "ScaleMaxArea", "F");
    g_TrackerParamsFieldIDs.ScaleStep_ID = env->GetFieldID(thizclass, "ScaleStep", "F");

    g_TrackerParamsFieldIDs.UpdateInterval_ID = env->GetFieldID(thizclass, "UpdateInterval", "I");
    g_TrackerParamsFieldIDs.PeakRatio_ID = env->GetFieldID(thizclass, "PeakRatio", "F");
    g_TrackerParamsFieldIDs.PSRThreshold_ID = env->GetFieldID(thizclass, "PSRThreshold", "F");
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_00024Bounds_InitJniFieldIDs
        (JNIEnv *env, jobject thiz) {
    jclass thizclass = env->GetObjectClass(thiz);
    g_BoundsFieldIDs.x0_ID = env->GetFieldID(thizclass, "x0", "I");
    g_BoundsFieldIDs.y0_ID = env->GetFieldID(thizclass, "y0", "I");
    g_BoundsFieldIDs.x1_ID = env->GetFieldID(thizclass, "x1", "I");
    g_BoundsFieldIDs.y1_ID = env->GetFieldID(thizclass, "y1", "I");
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_00024TrackerResult_InitJniFieldIDs
        (JNIEnv *env, jobject thiz) {
    jclass thizclass = env->GetObjectClass(thiz);
    g_TrackerResultFieldIDs.Succeed_ID = env->GetFieldID(thizclass, "Succeed", "Z");
    g_TrackerResultFieldIDs.Score_ID = env->GetFieldID(thizclass, "Score", "F");
    g_TrackerResultFieldIDs.Box_ID = env->GetFieldID(thizclass, "Box",
                                                     "Lcom.zjxdev.tracker.Bounds");
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_StartSystem
        (JNIEnv *env, jobject thiz, jstring logPath) {
    const char *path = env->GetStringUTFChars(logPath, NULL);
    CSRT::StartSystem(path);
    env->ReleaseStringUTFChars(logPath, path);
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_CloseSystem
        (JNIEnv *env, jobject thiz) {
    CSRT::CloseSystem();
}

extern "C"
JNIEXPORT jlong JNICALL Java_com_zjxdev_tracker_CSRT_CreateTracker
        (JNIEnv *env, jobject thiz, jint rows, jint cols, jobject params) {
    CSRT::CSRTrackerParams nativeParams;
    g_TrackerParamsFieldIDs.Convert(env, params, nativeParams);
    CSRT::CSRTracker *pTracker = new CSRT::CSRTracker(rows, cols, nativeParams);
    return (jlong) pTracker;
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_DeleteTracker
        (JNIEnv *env, jobject thiz, jlong trackerPtr) {
    CSRT::CSRTracker *pTracker = (CSRT::CSRTracker *) trackerPtr;
    if (pTracker != nullptr)
        delete pTracker;
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_InitializeTracker
        (JNIEnv *env, jobject thiz, jlong dataPtr, jobject bb, jlong trackerPtr) {
    unsigned char *pData = (unsigned char *) dataPtr;
    CSRT::CSRTracker *pTracker = (CSRT::CSRTracker *) trackerPtr;
    CSRT::Bounds nativebb;
    g_BoundsFieldIDs.Convert(env, bb, nativebb);
    pTracker->Initialize(pData, nativebb);
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_UpdateTracker
        (JNIEnv *env, jobject thiz, jlong dataPtr, jobject res, jlong trackerPtr) {
    unsigned char *pData = (unsigned char *) dataPtr;
    CSRT::CSRTracker *pTracker = (CSRT::CSRTracker *) trackerPtr;
    CSRT::Bounds nativebb;
    float score = 0.0f;
    bool succeed = pTracker->Update(pData, nativebb, score);
    g_TrackerResultFieldIDs.SetValues(env, res, succeed, score, nativebb);
}

extern "C"
JNIEXPORT void JNICALL Java_com_zjxdev_tracker_CSRT_ReinitialzieTracker
        (JNIEnv *env, jobject thiz, jlong trackerPtr) {
    CSRT::CSRTracker *pTracker = (CSRT::CSRTracker *) trackerPtr;
    pTracker->SetReinitialize();
}