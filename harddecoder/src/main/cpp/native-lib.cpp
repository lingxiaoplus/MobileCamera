#include <jni.h>
#include <string>
#include <omp.h>
#include <android/log.h>
#include "libyuv/include/libyuv.h"
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

using namespace libyuv;
void rotateI420(jbyte *input, jbyte *output, int width, int height,int rotation){
    RotationMode rotationMode = kRotate0;
    switch (rotation) {
        case 90:
            rotationMode = kRotate90;
            break;
        case 180:
            rotationMode = kRotate180;
            break;
        case 270:
            rotationMode = kRotate270;
            break;
    }
    I420Rotate((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, height,
               (uint8_t *)output + (width * height), height / 2,
               (uint8_t *)output + (width * height * 5 / 4), height / 2,
               width, height,
               rotationMode);
}

void mirrorI420(jbyte *input, jbyte *output,  jint width, jint height) {

    I420Mirror((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, height,
               (uint8_t *)output + (width * height), height / 2,
               (uint8_t *)output + (width * height * 5 / 4), height / 2,
               width, height);

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

extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21ToI420(JNIEnv *env, jclass type,
                                                             jbyteArray input_, jbyteArray output_,
                                                             jint width, jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);

    NV21ToI420((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width,
               (uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               width, height);

    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_RotateI420(JNIEnv *env, jclass type,
                                                             jbyteArray input_, jbyteArray output_,
                                                             jint width, jint height,
                                                             jint rotation) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);

    RotationMode rotationMode = kRotate0;
    switch (rotation) {
        case 90:
            rotationMode = kRotate90;
            break;
        case 180:
            rotationMode = kRotate180;
            break;
        case 270:
            rotationMode = kRotate270;
            break;
    }
    I420Rotate((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, height,
               (uint8_t *)output + (width * height), height / 2,
               (uint8_t *)output + (width * height * 5 / 4), height / 2,
               width, height,
               rotationMode);

    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21ToI420RotateAndConvertToNv12(JNIEnv *env,
                                                                                   jclass type,
                                                                                   jbyteArray input_,
                                                                                   jbyteArray output_,
                                                                                   jint width,
                                                                                   jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);

    NV21ToI420((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width,
               (uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               width, height);


    /*I420Rotate((const uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               (uint8_t *)input, height,
               (uint8_t *)input + (width * height), height / 2,
               (uint8_t *)input + (width * height * 5 / 4), height / 2,
               width, height,
               kRotate90);*/
    rotateI420(output,input,width,height,90);
    /*I420ToNV21((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, width ,
               (uint8_t *)output + (width * height), width,
               width, height);*/

    I420ToNV12((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, width ,
               (uint8_t *)output + (width * height), width,
               width, height);

    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21ToI420RotateAndMirrorConvertToNv12(JNIEnv *env,
                                                                                   jclass type,
                                                                                   jbyteArray input_,
                                                                                   jbyteArray output_,
                                                                                   jint width,
                                                                                   jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);

    NV21ToI420((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width,
               (uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               width, height);
    rotateI420(output,input,width,height,270);
    mirrorI420(input,output,width,height);
    I420ToNV12((const uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               (uint8_t *)input, width ,
               (uint8_t *)input + (width * height), width,
               width, height);

    jsize yuv_len = env->GetArrayLength(input_);
    memcpy(output,input,yuv_len);
    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}

