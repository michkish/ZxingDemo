package zxing.trustway.cn.ydjwzxing.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Zheming.xin on 2017/8/14.
 */

public class BitmapUtil {
    public static final String TAG = "BitmapUtil";

    public static String picPath = Environment.getExternalStorageDirectory().getPath() + "/ZxingBitmap";

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private static Rect framingRect, framingRectInPreview;
    public static Point cameraResolution;

    public static synchronized Rect getFramingRectInPreview(Point screenResolution, Point cameraResolution) {
        if (framingRectInPreview == null) {
            Rect framingRect = getFramingRect(screenResolution);
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

//      if (screenResolution.x / screenResolution.y != cameraResolution.x / cameraResolution.y ) {
//        rect.left = rect.left * cameraResolution.y / screenResolution.x;
//        rect.right = rect.right * cameraResolution.y / screenResolution.x;
//        rect.top = rect.top * cameraResolution.x / screenResolution.y;
//        rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
//      } else {
//        rect.left = rect.left * cameraResolution.x / screenResolution.x;
//        rect.right = rect.right * cameraResolution.x / screenResolution.x;
//        rect.top = rect.top * cameraResolution.y / screenResolution.y;
//        rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
//      }
            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;

            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }

    public static synchronized Rect getFramingRect(Point screenResolution) {
        if (framingRect == null) {
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);

            int length = width < height ? width : height;

            int leftOffset = (screenResolution.x - length) / 2;
            int topOffset = (screenResolution.y - length) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + length, topOffset + length);
        }
        return framingRect;
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    public static boolean saveBitmap(Bitmap mBitmap) {
        if (mBitmap == null) {
            return false;
        }

        String path = picPath + "/" + System.currentTimeMillis() + ".jpeg";
        Log.d(TAG, "save bitmap " + path);

        File mFile = new File(path);
        BufferedOutputStream os = null;
        FileOutputStream fos = null;
        Bitmap transformed = null;
        boolean isSuccess = false;
        try {
            boolean newFile = mFile.createNewFile();
            if (newFile) {
                fos = new FileOutputStream(mFile);
                os = new BufferedOutputStream(fos);
                Matrix m = new Matrix();

                m.setRotate(90);
//                if (cameraPosition == 0) {
//                    m.setRotate(270);
//                } else {
//                    m.setRotate(90);
//                }
                transformed = Bitmap.createBitmap(
                        mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), m, true
                );

                transformed.compress(Bitmap.CompressFormat.JPEG, 50, os);
                os.flush();
                isSuccess = true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            closeIO(os, fos);
//            if (isSuccess) {
//                //加入到缓存中
//                mBitmap.recycle();
//            }
        }
        return isSuccess;
    }

    public static boolean compressImageFromPhotos(byte[] bytes) {
//                        int x = bytes.length;
//                        Bitmap mBitmap1 = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        //压缩
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 只获取图片的大小信息，而不是将整张图片载入在内存中，避免内存溢出
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 2; // 默认像素压缩比例，压缩为原图的1/2
        int minLen = Math.min(height, width); // 原图的最小边长
        if (minLen > 100) { // 如果原始图像的最小边长大于100dp（此处单位我认为是dp，而非px）
            float ratio = (float) minLen / 100.0f; // 计算像素压缩比例
            inSampleSize = (int) ratio;
        }
        options.inJustDecodeBounds = false; // 计算好压缩比例后，这次可以去加载原图了
        options.inSampleSize = inSampleSize; // 设置为刚才计算的压缩比例
        Bitmap mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        if (mBitmap == null) {
            return false;
        }

        String path = picPath + "/" + System.currentTimeMillis() + ".jpeg";
        Log.d(TAG, "save bitmap " + path);

        File mFile = new File(path);
        BufferedOutputStream os = null;
        FileOutputStream fos = null;
        Bitmap transformed = null;
        boolean isSuccess = false;
        try {
            boolean newFile = mFile.createNewFile();
            if (newFile) {
                fos = new FileOutputStream(mFile);
                os = new BufferedOutputStream(fos);
                Matrix m = new Matrix();

                m.setRotate(90);
//                if (cameraPosition == 0) {
//                    m.setRotate(270);
//                } else {
//                    m.setRotate(90);
//                }
                transformed = Bitmap.createBitmap(
                        mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), m, true
                );

                transformed.compress(Bitmap.CompressFormat.JPEG, 50, os);
                os.flush();
                isSuccess = true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            closeIO(os, fos);
//            if (isSuccess) {
//                //加入到缓存中
//                mBitmap.recycle();
//            }
        }
        return isSuccess;
    }

    public static void closeIO(Closeable... cb){
        for (Closeable closeable : cb) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignored) {

                }
            }
        }

    }
}
