package com.camera.lingxiao.camerademo.crash;

/**
 * Created by lingxiao on 2017/12/26.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import com.camera.lingxiao.camerademo.utils.PrefUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/**
 * Created by lingxiao on 2017/8/11.
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    /**
     * 系统默认UncaughtExceptionHandler
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    /**
     * context
     */
    private Context mContext;
    /**
     * 存储异常和参数信息
     */
    private Map<String,String> paramsMap = new HashMap<>();
    /**
     * 格式化时间
     */
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private String TAG = this.getClass().getSimpleName();
    private static CrashHandler mInstance;
    private String path;
    private boolean mCrashToggle; //是否打开显示错误的activity
    private CrashHandler(){
    }
    /**
     *获取crashhandler实例
     */
    public static synchronized CrashHandler getInstance(){
        if (null == mInstance){
            mInstance = new CrashHandler();
        }
        return mInstance;
    }
    public void init(Context context,boolean crashToggle){
        this.mContext = context;
        this.mCrashToggle = crashToggle;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该handler为系统默认的
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    /**
     *uncaughtException 回调函数
     */
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (mDefaultHandler != null && !handleException(throwable)){
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread,throwable);
        }else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //跳转到崩溃提示activity  这个地方需要启动一个新的task，不然无法跳转
            if (mCrashToggle){
                Intent intent = new Intent(mContext,CrashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
            //退出程序
            AppManager.getAppManager().AppExit(mContext);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
    /**
     *自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     * @return true:处理了该异常信息，false：没有处理
     */
    private boolean handleException(Throwable throwable) {
        if (throwable == null){
            return false;
        }
        //收集设备参数信息
        collectDerviceInfo(mContext);
        //添加自定义信息
        addCustomInfo();
        //用toast提示异常
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                //Toast.makeText(mContext,"程序开小差了。。",Toast.LENGTH_SHORT).show();
                //showDialog(mContext);
                Looper.loop();
            }
        }).start();

        //保存日志信息
        saveCrashInfo2File(throwable);
        return true;
    }
    /**
     *保存错误信息到文件，便于上传到服务器
     * @param throwable
     */
    private String saveCrashInfo2File(Throwable throwable) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        Throwable cause = throwable.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            PrefUtils.setString(mContext, ContentValue.ERRORSTR,sb.toString());
            //long timestamp = System.currentTimeMillis();
            String time = format.format(new Date());
            //String fileName = "crash-" + time + "-" + timestamp + ".log";
            String fileName = "crash-" + time + ".log";
            //if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/crash/";
                //path = "/mnt/usb/sdb1";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(sb.toString().getBytes());
                fos.close();
            //}
            PrefUtils.setString(mContext, ContentValue.ERRORINFO,path+fileName);
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }
    /**
     *添加自定义参数
     */
    private void addCustomInfo() {

    }
    /**
     *收集设备信息
     */
    private void collectDerviceInfo(Context mContext) {
        //获取versionName,versionCode
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                paramsMap.put("versionName", versionName);
                paramsMap.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        //获取所有系统信息
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                paramsMap.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }
}
