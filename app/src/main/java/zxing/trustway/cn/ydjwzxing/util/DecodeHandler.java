package zxing.trustway.cn.ydjwzxing.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import zxing.trustway.cn.ydjwzxing.R;
import zxing.trustway.cn.ydjwzxing.listener.ZxingDecodeListener;

/**
 * Created by Zheming.xin on 2017/8/10.
 */

public class DecodeHandler extends Handler {
    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final MultiFormatReader multiFormatReader;
    private boolean running = true;

    private ZxingDecodeListener zxingDecodeListener;

    public DecodeHandler(Map<DecodeHintType,Object> hints, ZxingDecodeListener zxingDecodeListener) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
        this.zxingDecodeListener = zxingDecodeListener;
    }

    @Override
    public void handleMessage(Message message) {
        if (message == null || !running) {
            return;
        }
        switch (message.what) {
            case R.id.decode:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case R.id.quit:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        Result rawResult = null;
        PlanarYUVLuminanceSource source = zxingDecodeListener.buildLuminanceSource(data, width, height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }

//        Handler handler = activity.getHandler();
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            long end = System.currentTimeMillis();
            Log.d(TAG, "Found barcode in " + (end - start) + " ms");
            Bundle bundle = new Bundle();
            bundleThumbnail(source, bundle);
            zxingDecodeListener.decodeSucceeded(bundle, R.id.decode_succeeded, rawResult);
            /*if (handler != null) {
                Bundle bundle = new Bundle();
                bundleThumbnail(source, bundle);
                Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
                message.setData(bundle);
                message.sendToTarget();
            }*/
        } else {
            zxingDecodeListener.decodeFailed(R.id.decode_failed);
            /*if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }*/
        }
    }

    private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
        int[] pixels = source.renderThumbnail();
        int width = source.getThumbnailWidth();
        int height = source.getThumbnailHeight();
        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
        bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
    }
}
