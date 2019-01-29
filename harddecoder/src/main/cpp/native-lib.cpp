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
    int limit_u = width * height;
    int limit_v = width * height / 4 + limit_u;
    I420Rotate((const uint8_t *)input, width,
               (uint8_t *)input + limit_u, width / 2,
               (uint8_t *)input + limit_v, width / 2,
               (uint8_t *)output, height,
               (uint8_t *)output + limit_u, height / 2,
               (uint8_t *)output + limit_v, height / 2,
               width, height,
               rotationMode);
}

void mirrorI420(jbyte *input, jbyte *output,  int width, int height) {
    int limit_u = width * height;
    int limit_v = width * height / 4 + limit_u;
    I420Mirror((const uint8_t *)input, width,
               (uint8_t *)input + limit_u, width >> 1,
               (uint8_t *)input + limit_v, width >> 1,

               (uint8_t *)output, width,
               (uint8_t *)output + limit_u, width >> 1,
               (uint8_t *)output + limit_v, width  >> 1,

               width, height);
}

void I420ToArgb(jbyte *input, jbyte *output, int width, int height) {
    int limit_u = width * height;
    int limit_v = width * height / 4 + limit_u;
    I420ToARGB((const uint8_t *)input, width,
               (uint8_t *)input + limit_u, width / 2,
               (uint8_t *)input + limit_v, width / 2,
               (uint8_t *)output,width*height*4,width,height);
}

void I420ToNV12(jbyte *input, jbyte *output, int width, int height){
    I420ToNV12((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, width ,
               (uint8_t *)output + (width * height), width,
               width, height);
}
void NV21ToI420(jbyte *input, jbyte *output, int width, int height){
    int limit_u = width * height;
    int limit_v = width * height / 4 + limit_u;
    NV21ToI420((const uint8_t *)input, width,
               (uint8_t *)input + limit_u, width,
               (uint8_t *)output, width,
               (uint8_t *)output + limit_u, width / 2,
               (uint8_t *)output + limit_v, width / 2,
               width, height);
}


void mirror(jbyte *input,int width, int height) {
    //copy origin data
    unsigned char *  origdata = NULL;
    unsigned char * Dst_data = (unsigned char *)(input);
    int size = width * height * 3 / 2;
    origdata = (unsigned char *)calloc(1,size);

    memcpy(origdata, input, size);

    //YUV420 image size
    int I420_Y_Size = width * height;
    int I420_U_Size = (width >> 1) * (height >> 1);
//    int I420_V_Size = I420_U_Size;

    unsigned char *Y_data_src = origdata;
    unsigned char *U_data_src = origdata + I420_Y_Size ;
    unsigned char *V_data_src = origdata + I420_Y_Size + I420_U_Size;


    int Src_Stride_Y = width;
    int Src_Stride_U = (width+1) >> 1;
    int Src_Stride_V = Src_Stride_U;

    //最终写入目标
    unsigned char *Y_data_Dst_rotate = Dst_data;
    unsigned char *U_data_Dst_rotate = Dst_data + I420_Y_Size;
    unsigned char *V_data_Dst_rotate = Dst_data + I420_Y_Size + I420_U_Size;

    //mirro
    int Dst_Stride_Y_mirror = width;
    int Dst_Stride_U_mirror = (width+1) >> 1;
    int Dst_Stride_V_mirror = Dst_Stride_U_mirror;
    I420Mirror(Y_data_src, Src_Stride_Y,
                             U_data_src, Src_Stride_U,
                             V_data_src, Src_Stride_V,
                             Y_data_Dst_rotate, Dst_Stride_Y_mirror,
                             U_data_Dst_rotate, Dst_Stride_U_mirror,
                             V_data_Dst_rotate, Dst_Stride_V_mirror,
               width, height);

    free((void**)&origdata);
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
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21RotateAndConvertToNv12(JNIEnv *env,
                                                                                   jclass type,
                                                                                   jbyteArray input_,
                                                                                   jbyteArray output_,
                                                                                   jint width,
                                                                                   jint height,jint rotation) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);

    NV21ToI420((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width,
               (uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               width, height);
    rotateI420(output,input,width,height,rotation);
    /*I420ToNV21((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width / 2,
               (uint8_t *)input + (width * height * 5 / 4), width / 2,
               (uint8_t *)output, width ,
               (uint8_t *)output + (width * height), width,
               width, height);*/
    I420ToNV12(input,output,width,height);
    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21RotateAndMirrorConvertToNv12(JNIEnv *env,
                                                                                   jclass type,
                                                                                   jbyteArray input_,
                                                                                   jbyteArray output_,
                                                                                   jint width,
                                                                                   jint height,jint rotation) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);

    NV21ToI420((const uint8_t *)input, width,
               (uint8_t *)input + (width * height), width,
               (uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               (uint8_t *)output + (width * height * 5 / 4), width / 2,
               width, height);
    //mirror(output,width,height);
    mirrorI420(output,input,width,height);  //先镜像再旋转，不然会出现转换错误的问题  暂时不清楚原==
    rotateI420(input,output,width,height,rotation);
    I420ToNV12(output,input,width,height);

    jsize yuv_len = env->GetArrayLength(input_);
    memcpy(output,input,yuv_len);

    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_NV21ToARGB(JNIEnv *env, jclass type,
                                                             jbyteArray input_, jbyteArray output_,
                                                             jint width, jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);
    jsize yuv_len = env->GetArrayLength(input_);
    NV21ToI420(input,output,width,height);
    I420ToArgb(output,input,width,height);
    memcpy(output,input,yuv_len);
    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_media_lingxiao_harddecoder_utils_YuvUtil_ARGBToNV21(JNIEnv *env, jclass type,
                                                             jbyteArray input_, jbyteArray output_,
                                                             jint width, jint height) {
    jbyte *input = env->GetByteArrayElements(input_, NULL);
    jbyte *output = env->GetByteArrayElements(output_, NULL);
    jsize argb_len = env->GetArrayLength(input_);

    ARGBToNV21((const uint8_t *)input,width*height*4,
               (uint8_t *)output, width,
               (uint8_t *)output + (width * height), width / 2,
               width, height);
    env->ReleaseByteArrayElements(input_, input, 0);
    env->ReleaseByteArrayElements(output_, output, 0);
}