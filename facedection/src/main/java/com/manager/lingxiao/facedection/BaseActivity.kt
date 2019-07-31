package com.manager.lingxiao.facedection

import android.annotation.SuppressLint
import android.app.ProgressDialog

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

abstract class BaseActivity : AppCompatActivity() ,EasyPermissions.PermissionCallbacks{

    private var mPmanager: PackageManager? = null
    private var versionCode: Int = 0
    private var mBarcolor: Int = 0

    private var progressDialog: ProgressDialog? = null

    /**
     * 得到当前界面的资源文件id
     */
    protected abstract val contentLayoutId: Int
    /**
     * 初始化dagger注入
     */
    protected open fun initInject(){

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        /**
         * 设置为横屏
         */
        if(getRequestedOrientation()!= ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onCreate(savedInstanceState)
        //在界面未初始化之前调用的初始化窗口
        initWindows()
        if (initArgs(intent.extras)) {
            // 得到界面Id并设置到Activity界面中
            val layId = contentLayoutId
            if (layId != 0) {
                setContentView(layId)
            }
            initInject()
            initBefore()
            initWidget()
            initData()
        } else {
            finish()
        }
    }

    /**
     * 初始化控件调用之前
     */
    protected open fun initBefore() {

    }

    /**
     * 初始化
     */
    private fun initWindows() {

    }

    /**
     * 初始化相关参数
     * 如果参数正确返回True，错误返回False
     */
    protected fun initArgs(bundle: Bundle?): Boolean {
        return true
    }

    /**
     * 初始化控件
     */
    protected open fun initWidget() {
        //unBinder = ButterKnife.bind(this)
        //initSubscription()
    }

    /**
     * 初始化数据
     */
    protected fun initData() {

    }

    /**
     * 皮肤改变调用
     */
    protected open fun isSkinChanged() :Boolean{
        return true
    }

    fun StartActivity(clzz: Class<*>, isFinish: Boolean) {
        startActivity(Intent(applicationContext, clzz))
        if (isFinish) {
            finish()
        }
    }


    /**
     * 设置toolbar的返回键
     */
    fun setToolbarBack(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
        }
        //toolbar.title = ""
    }

    //跳转到网页
    fun goToInternet(context: Context, marketUrl: String) {
        val uri = Uri.parse(marketUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            //拒绝授权后，从系统设置了授权后，返回APP进行相应的操作
        }
    }
    override fun onBackPressed() {
        // 得到当前Activity下的所有Fragment
        @SuppressLint("RestrictedApi")
        val fragments = supportFragmentManager.fragments
        // 判断是否为空
        if (fragments.size > 0) {
            for (fragment in fragments) {
                // 判断是否为我们能够处理的Fragment类型
                /*if (fragment is BaseFragment) {
                    // 判断是否拦截了返回按钮
                    if (fragment.onBackPressed()) {
                        // 如果有直接Return
                        return
                    }
                }*/
            }
        }
        super.onBackPressed()
    }


    /**
     * 是否注册事件分发
     *
     * @return true绑定EventBus事件分发，默认不绑定，子类需要绑定的话复写此方法返回true.
     */
    protected open fun isRegisterEventBus() :Boolean{
        return false
    }
    /**
     * 当点击界面导航返回时，Finish当前界面
     * @return
     */
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancleProgressDialog()
    }

    protected fun setSwipeColor(swipeLayout: SwipeRefreshLayout) {
        swipeLayout.setColorSchemeResources(
            R.color.colorPrimary,
            android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_green_light
        )
    }


    /**
     * 显示进度条
     */
    fun showProgressDialog(msg: String) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage(msg)
        progressDialog?.show()
    }

    fun cancleProgressDialog() {
        progressDialog?.dismiss()
    }
}
