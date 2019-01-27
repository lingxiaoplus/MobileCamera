package com.media.lingxiao.harddecoder.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by lingxiao on 2017/9/18.
 */

public class BitmapUtils {
    private static final String TAG = BitmapUtils.class.getSimpleName();
    /**
     * @description 计算图片的压缩比率
     *
     * @param options 参数
     * @param reqWidth 目标的宽度
     * @param reqHeight 目标的高度
     * @return
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * @description 通过传入的bitmap，进行压缩，得到符合标准的bitmap
     *
     * @param src
     * @param dstWidth
     * @param dstHeight
     * @return
     */
    private static Bitmap createScaleBitmap(Bitmap src, int dstWidth, int dstHeight, int inSampleSize) {
        if (null == src){
            return null;
        }
        if (dstWidth <0|| dstHeight<0){
            return null;
        }
        // 如果是放大图片，filter决定是否平滑，如果是缩小图片，filter无影响，我们这里是缩小图片，所以直接设置为false
        Bitmap dst = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, false);
        if (src != dst) { // 如果没有缩放，那么不回收
            src.recycle(); // 释放Bitmap的native像素数组
        }
        return dst;
    }

    /**
     * @description 从Resources中加载图片
     *
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 设置成了true,不占用内存，只获取bitmap宽高
        BitmapFactory.decodeResource(res, resId, options); // 读取图片长宽，目的是得到图片的宽高
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight); // 调用上面定义的方法计算inSampleSize值
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeResource(res, resId, options); // 载入一个稍大的缩略图
        return createScaleBitmap(src, reqWidth, reqHeight, options.inSampleSize); // 通过得到的bitmap，进一步得到目标大小的缩略图
    }

    /**
     * @description 从SD卡上加载图片
     *
     * @param pathName
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromFile(String pathName, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeFile(pathName, options);
        return createScaleBitmap(src, reqWidth, reqHeight, options.inSampleSize);
    }

    /**
     * 旋转图片
     *
     * @param angle  被旋转角度
     * @param bitmap 图片对象
     * @return 旋转后的图片
     */
    public static Bitmap rotaingBitmap(int angle, Bitmap bitmap) {
        //bitmap = small(bitmap);   不缩放
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        returnBm = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return returnBm;
    }


    /**
     * 将Bitmap转换成文件
     * 保存文件
     *
     * @param bm
     * @param filename
     * @throws IOException
     */
    public static File saveFile(Bitmap bm, String filename, String filepath) throws IOException {
        File file = new File(filepath + filename);
        if (file.exists()) {
            file.delete();
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        bos.flush();
        bos.close();
        return file;
    }

    /**
     * 图片是横屏还是竖屏
     * @param path
     * @return
     */
    public static boolean isLandscape(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 设置成了true,不占用内存，只获取bitmap宽高
        BitmapFactory.decodeFile(path, options); // 读取图片长宽，目的是得到图片的宽高
        if (options.outWidth > options.outHeight){
            return true;
        }
        return false;
    }
    /**
     * 将调用系统相册的图片进行压缩
     *
     * @param contentResolver
     * @param uri
     * @return
     */
    public static Bitmap getBitmapFormUri(ContentResolver contentResolver, Uri uri, float width, float height) {
        try {
            InputStream input = contentResolver.openInputStream(uri);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;//optional
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();
            int originalWidth = onlyBoundsOptions.outWidth;
            int originalHeight = onlyBoundsOptions.outHeight;
            if ((originalWidth == -1) || (originalHeight == -1))
                return null;
            //图片分辨率以480x800为标准
            //float hh = 800f;//这里设置高度为800f
            //float ww = 480f;//这里设置宽度为480f
            //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
            int be = 1;//be=1表示不缩放
            if (originalWidth > originalHeight && originalWidth > width) {//如果宽度大的话根据宽度固定大小缩放
                be = (int) (originalWidth / width);
            } else if (originalWidth < originalHeight && originalHeight > height) {//如果高度高的话根据宽度固定大小缩放
                be = (int) (originalHeight / height);
            }
            if (be <= 0)
                be = 1;
            //比例压缩
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = be;//设置缩放比例
            bitmapOptions.inDither = true;//optional
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            input = contentResolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();

            return compressImageByQuality(bitmap,1024);//再进行质量压缩
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过分辨率压缩bitmap
     * @param bitmap
     * @param width 需要压缩的尺寸
     * @param height
     * @return
     */
    public static Bitmap compressImageByResolution(Bitmap bitmap,float width,float height){
        try {
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            int originalWidth = onlyBoundsOptions.outWidth;
            int originalHeight = onlyBoundsOptions.outHeight;
            if ((originalWidth == -1) || (originalHeight == -1))
                return null;
            //图片分辨率以480x800为标准
            //float hh = 800f;//这里设置高度为800f
            //float ww = 480f;//这里设置宽度为480f
            //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
            float be = 1;//be=1表示不缩放
            if (originalWidth > originalHeight && originalWidth > width) {//如果宽度大的话根据宽度固定大小缩放
                be =  (originalWidth / width);
            } else if (originalWidth < originalHeight && originalHeight > height) {//如果高度高的话根据宽度固定大小缩放
                be =  (originalHeight / height);
            }
            if (be <= 0)
                be = 1;

            //比例压缩
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = (int) be;//设置缩放比例
            bitmapOptions.inDither = true;
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            ByteArrayInputStream input = new ByteArrayInputStream(baos.toByteArray());

            bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);

            return bitmap;//再进行质量压缩
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 通过质量压缩bitmap
     * @param image
     * @return
     */
    public static Bitmap compressImageByQuality(Bitmap image,int size) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        int options = 100;
        //循环判断如果压缩后图片是否大于1000kb,大于继续压缩
        while (baos.toByteArray().length / 1024 > size) {
            baos.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        //把压缩后的数据baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        //把ByteArrayInputStream数据生成图片
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);

        if (image != null && !image.isRecycled()){
            image.recycle();
            image = null;
        }
        try {
            baos.close();
            isBm.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap zoomImage(Bitmap bigmage) {
        // 获取这个图片的宽和高
        float width = bigmage.getWidth();
        float height = bigmage.getHeight();
        float maxSize = Math.max(width,height);
        float scale;
        double newWidth = 1920;
        double newHeight = 1080;
        if (maxSize > 1920){
            if (width > height){
                scale = width/height;
                if (scale >1.5f){
                    newWidth = 1920;
                    newHeight = 1080;
                }else if (scale >1.0f){
                    newWidth = 1440;
                    newHeight = 1080;
                }
            }else {
                scale = height/width;
                if (scale >1.5f){
                    newWidth = 1080;
                    newHeight = 1920;
                }else if (scale >1.0f){
                    newWidth = 1080;
                    newHeight = 1440;
                }
            }
        }else {
            return bigmage;
        }
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(bigmage, 0, 0, (int) width,
                (int) height, matrix, true);
        return bitmap;
    }

    /**
     * 获取照片旋转角度
     * @param filepath
     * @return
     */
    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            Log.d(TAG,"cannot read exif" + ex);
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
            }
        }
        return degree;
    }
    /**
     * 按正方形裁切图片
     */
    public static Bitmap ImageCrop(Bitmap bitmap,  Rect rect) {
        if (bitmap == null) {
            return null;
        }
        int top = rect.top;
        int left = rect.left;  //取左上角
        int bottom = rect.bottom;
        int right = rect.right;
        int scaleW = right - left;
        int scaleH = bottom - top;
        Bitmap bmp = Bitmap.createBitmap(bitmap, left, top, scaleW, scaleH, null,
                false);

        return bmp;

    }

    /**
     * 获取本地图片
     */
    private Bitmap getLocationImgFile(String path) {
        //图片长宽缩小1/2倍
        //Bitmap bitmap = BitmapFactory.decodeFile(mFilePath, getBitmapOption(2));
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        return bitmap;
    }

    private BitmapFactory.Options getBitmapOption(int inSampleSize) {
        System.gc();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inSampleSize = inSampleSize;
        return options;
    }


    /**
     * 模糊图像
     * @param bitmap
     * @param radius 模糊程度 0<radius<=25
     * @param context
     * @return
     */
    public static Bitmap blurBitmap(Bitmap bitmap, float radius, Context context) {
        RenderScript rs = RenderScript.create(context);
        //Create allocation from Bitmap bitmap中的数据装填
        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        Type t = allocation.getType();
        //Create allocation with the same type 与第一个allocation的大小和type都相同多2D数组
        Allocation blurredAllocation = Allocation.createTyped(rs, t);
        //Create script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        //Set blur radius (maximum 25.0)
        blurScript.setRadius(radius);
        //Set input for script
        blurScript.setInput(allocation);
        //Call script for output allocation
        blurScript.forEach(blurredAllocation);
        //Copy script result into bitmap
        blurredAllocation.copyTo(bitmap);
        //Destroy everything to free memory
        allocation.destroy();
        blurredAllocation.destroy();
        blurScript.destroy();
        rs.destroy();
        return bitmap;
    }


    /**
     * yuv转bitmap
     * @param context
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static Bitmap YuvConvertToBitmap(Context context, byte[] data, int width, int height) {
        Type.Builder yuvType = null, rgbaType;
        Allocation in = null, out = null;
        try {
            final int w = width;  //宽度
            final int h = height;
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                RenderScript rs = RenderScript.create(context);
                ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
                if (yuvType == null) {
                    yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                    in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                    rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h);
                    out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
                }

                in.copyFrom(data);
                yuvToRgbIntrinsic.setInput(in);
                yuvToRgbIntrinsic.forEach(out);
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                out.copyTo(bitmap);

                rs.destroy();
                in.destroy();
                out.destroy();
                yuvToRgbIntrinsic.destroy();
            } else {
                //速度很慢
                ByteArrayOutputStream baos;
                byte[] rawImage;
                //处理data
                BitmapFactory.Options newOpts = new BitmapFactory.Options();
                newOpts.inJustDecodeBounds = true;
                YuvImage yuvimage = new YuvImage(
                        data,
                        ImageFormat.NV21,
                        w,
                        h,
                        null);
                baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, w, h), 100, baos);// 80--JPG图片的质量[0-100],100最高
                rawImage = baos.toByteArray();
                //将rawImage转换成bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
            }
            in = null;
            out = null;
            return bitmap;
        } catch (Throwable e) {
            in = null;
            out = null;
            return null;
        }
    }

}
