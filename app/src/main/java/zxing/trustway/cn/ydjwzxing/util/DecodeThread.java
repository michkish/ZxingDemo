package zxing.trustway.cn.ydjwzxing.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPointCallback;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import zxing.trustway.cn.ydjwzxing.listener.ZxingDecodeListener;

/**
 * Created by Zheming.xin on 2017/8/10.
 */

public class DecodeThread extends Thread {
    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

    private final Map<DecodeHintType,Object> hints;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    private ZxingDecodeListener zxingDecodeListener;

    public DecodeThread(Collection<BarcodeFormat> decodeFormats,
                 Map<DecodeHintType,?> baseHints,
                 String characterSet,
                 ResultPointCallback resultPointCallback, ZxingDecodeListener zxingDecodeListener) {

        handlerInitLatch = new CountDownLatch(1);

        this.zxingDecodeListener = zxingDecodeListener;

        hints = new EnumMap<>(DecodeHintType.class);
        if (baseHints != null) {
            hints.putAll(baseHints);
        }

        // The prefs can't change while the thread is running, so pick them up once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {
            decodeFormats = EnumSet.noneOf(BarcodeFormat.class);

            /* now only decode QR*/
//            decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
//            decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
//            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
//            decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
//            decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
        Log.i("DecodeThread", "Hints: " + hints);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(hints, zxingDecodeListener);
        handlerInitLatch.countDown();
        Looper.loop();
    }
}
