#pragma version(1)
#pragma rs java_package_name(com.zjxdev.tracker)
#pragma rs_fp_relaxed

rs_allocation gInputFrame;
rs_allocation gOuputFrame;
uint32_t gWidth;
uint32_t gHeight;

uchar4 RS_KERNEL yuv2rgba_swapflip(uchar4 prevPixel, uint32_t x, uint32_t y)
{
    uchar4 curPixel;
    curPixel.r = rsGetElementAtYuv_uchar_Y(gInputFrame, x, y);
    curPixel.g = rsGetElementAtYuv_uchar_U(gInputFrame, x, y);
    curPixel.b = rsGetElementAtYuv_uchar_V(gInputFrame, x, y);
    uchar4 out = rsYuvToRGBA_uchar4(curPixel.r, curPixel.g, curPixel.b);
    out.a = 255;
    uint32_t nx = gHeight - 1 - y;
    uint32_t ny = gWidth - 1 - x;
    rsSetElementAt_uchar4(gOuputFrame, out, nx, ny);
    return out;
}

uchar4 RS_KERNEL yuv2rgba_swap(uchar4 prevPixel, uint32_t x, uint32_t y)
{
    uchar4 curPixel;
    curPixel.r = rsGetElementAtYuv_uchar_Y(gInputFrame, x, y);
    curPixel.g = rsGetElementAtYuv_uchar_U(gInputFrame, x, y);
    curPixel.b = rsGetElementAtYuv_uchar_V(gInputFrame, x, y);
    uchar4 out = rsYuvToRGBA_uchar4(curPixel.r, curPixel.g, curPixel.b);
    out.a = 255;
    uint32_t nx = gHeight - 1 - y;
    uint32_t ny = x;
    rsSetElementAt_uchar4(gOuputFrame, out, nx, ny);
    return out;
}

uchar4 RS_KERNEL yuv2rgba_flip(uchar4 prevPixel, uint32_t x, uint32_t y)
{
    uchar4 curPixel;
    curPixel.r = rsGetElementAtYuv_uchar_Y(gInputFrame, x, y);
    curPixel.g = rsGetElementAtYuv_uchar_U(gInputFrame, x, y);
    curPixel.b = rsGetElementAtYuv_uchar_V(gInputFrame, x, y);
    uchar4 out = rsYuvToRGBA_uchar4(curPixel.r, curPixel.g, curPixel.b);
    out.a = 255;
    rsSetElementAt_uchar4(gOuputFrame, out, x, gHeight - 1 - y);
    return out;
}

uchar4 RS_KERNEL yuv2rgba(uchar4 prevPixel, uint32_t x, uint32_t y)
{
    uchar4 curPixel;
    curPixel.r = rsGetElementAtYuv_uchar_Y(gInputFrame, x, y);
    curPixel.g = rsGetElementAtYuv_uchar_U(gInputFrame, x, y);
    curPixel.b = rsGetElementAtYuv_uchar_V(gInputFrame, x, y);
    uchar4 out = rsYuvToRGBA_uchar4(curPixel.r, curPixel.g, curPixel.b);
    out.a = 255;
    rsSetElementAt_uchar4(gOuputFrame, out, x, y);
    return out;
}