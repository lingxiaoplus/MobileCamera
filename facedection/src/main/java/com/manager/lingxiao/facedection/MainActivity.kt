package com.manager.lingxiao.facedection

import android.Manifest
import android.view.WindowManager
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.locks.ReentrantLock

class MainActivity : BaseActivity() {
    val RC_CAMERA_AND_LOCATION = 1
    private val perms = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO)

    override val contentLayoutId: Int
        get() = R.layout.activity_main


    override fun initWidget() {
        super.initWidget()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        methodRequiresTwoPermission()
    }

    private fun methodRequiresTwoPermission() {
        if (!EasyPermissions.hasPermissions(this, *perms)) {
            EasyPermissions.requestPermissions(this, "需要同意权限",
                    RC_CAMERA_AND_LOCATION, *perms)
        } else {

        }
    }
}
