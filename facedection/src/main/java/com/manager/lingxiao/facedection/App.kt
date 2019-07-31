package com.manager.lingxiao.facedection

import android.app.Application
import android.util.Log
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.OpenCVLoader


class App :Application(){
    val TAG = App::class.java.simpleName
    override fun onCreate() {
        super.onCreate()
        initOpenCv()
    }

    private fun initOpenCv() {
        val loaderCallback = object : BaseLoaderCallback(applicationContext) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.e(TAG, "OpenCV loaded successfully")
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }

            override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface) {

            }
        }
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getApplicationContext(), loaderCallback);
        } else {
            Log.e(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}