#include <jni.h>
#include <string>
#include <omp.h>
#include <android/log.h>
#define LOG_TAG "FACESDK"

#define DEBUG
#define ANDROID_PLATFORM

#ifdef DEBUG
	#ifdef ANDROID_PLATFORM
		#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
		#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
		#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
		#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
	#else
		#define LOGD(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
		#define LOGI(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
		#define LOGW(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
		#define LOGE(fmt, ...) printf(fmt"\n", ##__VA_ARGS__)
	#endif
#else
	#define LOGD(...)
	#define LOGI(...)
	#define LOGW(...)
	#define LOGE(...)
#endif

void nv21_clip(const char* yuv, int yuvW, int yuvH, int clipRectLeft, int clipRectTop, int clipRectW, int clipRectH, char* yuv_out_buffer) {
	int yLen = yuvW * yuvH;
	#pragma omp parallel for
	for(int i = 0; i < clipRectH; ++i) {
		memcpy(yuv_out_buffer + i * clipRectW, yuv + clipRectLeft + (clipRectTop + i) * yuvW, clipRectW);
	}
	int dstYLen = clipRectW * clipRectH;
	#pragma omp parallel for
	for(int i = 0; i < clipRectH >> 1; ++i) {
		memcpy(yuv_out_buffer + dstYLen + i * clipRectW, yuv + yLen + (clipRectTop / 2 + i) * yuvW + clipRectLeft, clipRectW);
	}
}


void cutYuv(unsigned char *tarYuv, unsigned char *srcYuv, int startW,
            int startH, int cutW, int cutH, int srcW, int srcH)
{
  int i;
  int j = 0;
  int k = 0;
  //分配一段内存，用于存储裁剪后的Y分量
  unsigned char *tmpY = (unsigned char *)malloc(cutW*cutH);
  //分配一段内存，用于存储裁剪后的UV分量
	unsigned char *tmpUV = (unsigned char *)malloc(cutW*cutH/2);
  for(i=startH; i<cutH+startH; i++) {
             // 逐行拷贝Y分量，共拷贝cutW*cutH
             memcpy(tmpY+j*cutW, srcYuv+startW+i*srcW, cutW);
             j++;
           }
           for(i=startH/2; i<(cutH+startH)/2; i++) {
             //逐行拷贝UV分量，共拷贝cutW*cutH/2
             memcpy(tmpUV+k*cutW, srcYuv+startW+srcW*srcH+i*srcW, cutW);
             k++;
           }
           //将拷贝好的Y，UV分量拷贝到目标内存中
           memcpy(tarYuv, tmpY, cutW*cutH);
           memcpy(tarYuv+cutW*cutH, tmpUV, cutW*cutH/2);
           free(tmpY);
           free(tmpUV);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_rotateYuv90(JNIEnv *env,
                                                         jclass instance,
                                                         jbyteArray yuv420p,
                                                         jint imageWidth, jint imageHeight){
    jbyte *data = env->GetByteArrayElements(yuv420p, NULL);
    jbyteArray yuvArray = env->NewByteArray(imageWidth * imageHeight * 3 / 2);
    jbyte *yuv = env->GetByteArrayElements(yuvArray, NULL);
    // Rotate the Y luma
    int i = 0;
    for (int x = 0; x < imageWidth; x++) {
        for (int y = imageHeight - 1; y >= 0; y--) {
            yuv[i] = data[y * imageWidth + x];
            i++;
        }
    }
    // Rotate the U and V color components
    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (int x = imageWidth - 1; x > 0; x = x - 2) {
        for (int y = 0; y < imageHeight / 2; y++) {
            yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
            i--;
            yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
            i--;
        }
    }

    env->ReleaseByteArrayElements(yuv420p, data, 0);
    env->ReleaseByteArrayElements(yuvArray,yuv,0);
    return yuvArray;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_rotateYUVDegree270AndMirror(JNIEnv *env, jclass type,
                                                                        jbyteArray nv21_,
                                                                        jint imageWidth, jint imageHeight) {
    jbyte *data = env->GetByteArrayElements(nv21_, NULL);
    jbyteArray yuvArray = env->NewByteArray(imageWidth * imageHeight * 3 / 2);
    jbyte *yuv = env->GetByteArrayElements(yuvArray, NULL);
    // Rotate and mirror the Y luma
    int i = 0;
    int maxY = 0;

    for (int x = imageWidth - 1; x >= 0; x--) {
        maxY = imageWidth * (imageHeight - 1) + x * 2;
        for (int y = 0; y < imageHeight; y++) {
            yuv[i] = data[maxY - (y * imageWidth + x)];
            i++;
        }
    }
    // Rotate and mirror the U and V color components
    int uvSize = imageWidth * imageHeight;
    i = uvSize;
    int maxUV = 0;
    for (int x = imageWidth - 1; x > 0; x = x - 2) {
        maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize;
        for (int y = 0; y < imageHeight / 2; y++) {
            yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)];
            i++;
            yuv[i] = data[maxUV - (y * imageWidth + x)];
            i++;
        }
    }
    env->ReleaseByteArrayElements(nv21_, data, 0);
    return yuvArray;
}

void detailPic90(uint8_t* yuv_origin, uint8_t* yuv_temp, int nw, int nh, int w, int h) {
    int deleteW = (nw - h) / 2;
    int deleteH = (nh - w) / 2;
    int i, j;
    for (i = 0; i < h; i++){
        for (j = 0; j < w; j++){
            yuv_temp[(h- i) * w - 1 - j] = yuv_origin[nw * (deleteH + j) + nw - deleteW
            -i];
        }
    }
    int index = w * h;
    for (i = deleteW + 1; i< nw - deleteW; i += 2){
        for (j = nh / 2 * 3 -deleteH / 2; j > nh + deleteH / 2; j--){
            yuv_temp[index++] = yuv_origin[(j - 1) * nw + i];
        }
    }
    for (i = deleteW; i < nw- deleteW; i += 2){
        for (j = nh / 2 * 3 -deleteH / 2; j > nh + deleteH / 2; j--){
            yuv_temp[index++] = yuv_origin[(j - 1) * nw + i];
        }
    }
}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_setBitmapBits(JNIEnv *env,
                                                              jobject instance,
                                                              jbyteArray bgr_,
                                                              jint width,
                                                              jint height) {
    jbyte *bgr = env->GetByteArrayElements(bgr_, NULL);
    jbyteArray out = env->NewByteArray(width * height * 4);
    jbyte* outPtr = env->GetByteArrayElements(out, NULL);

    int wh = width * height;
//#pragma omp parallel for
    for(int i = 0; i < wh; ++i) {
        outPtr[i*4] =  bgr[i*3+2];
        outPtr[i*4+1] = bgr[i*3+1];
        outPtr[i*4+2] = bgr[i*3];
        outPtr[i*4+3] = 255;
    }

    env->ReleaseByteArrayElements(out, outPtr, 0);
    env->ReleaseByteArrayElements(bgr_, bgr, 0);

    return out;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21ToNV12(JNIEnv *env, jclass type, jbyteArray nv21_,
                                                       jbyteArray nv12_, jint width, jint height) {
    jbyte *nv21 = env->GetByteArrayElements(nv21_, NULL);
    jbyte *nv12 = env->GetByteArrayElements(nv12_, NULL);

    if (nv21 == NULL || nv12 == NULL) return;
    int framesize = width * height;
    int i = 0, j = 0;
    memcpy(nv12,nv21,width * height);
    for (i = 0; i < framesize; i++) {
        nv12[i] = nv21[i];
    }
    for (j = 0; j < framesize / 2; j += 2) {
        nv12[framesize + j - 1] = nv21[j + framesize];
    }
    for (j = 0; j < framesize / 2; j += 2) {
        nv12[framesize + j] = nv21[j + framesize - 1];
    }
    env->ReleaseByteArrayElements(nv21_, nv21, 0);
    env->ReleaseByteArrayElements(nv12_, nv12, 0);
}
