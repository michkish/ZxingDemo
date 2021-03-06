package zxing.trustway.cn.ydjwzxing.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;

import java.util.Collection;
import java.util.Map;

import zxing.trustway.cn.ydjwzxing.R;
import zxing.trustway.cn.ydjwzxing.listener.ViewfinderResultPointCallback;
import zxing.trustway.cn.ydjwzxing.listener.ZxingDecodeListener;
import zxing.trustway.cn.ydjwzxing.widget.ViewfinderView;

/**
 * Created by Zheming.xin on 2017/8/10.
 */

public class ContextDecodeHandler extends Handler {
    private static final String TAG = ContextDecodeHandler.class.getSimpleName();

    private final DecodeThread decodeThread;
    private State state;
    private ZxingDecodeListener zxingDecodeListener;

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    public ContextDecodeHandler(Collection<BarcodeFormat> decodeFormats,
                         Map<DecodeHintType,?> baseHints,
                         String characterSet, ViewfinderView view, ZxingDecodeListener zxingDecodeListener) {
        decodeThread = new DecodeThread(decodeFormats, baseHints, characterSet,
                new ViewfinderResultPointCallback(view), zxingDecodeListener);
        decodeThread.start();
        state = State.SUCCESS;
        this.zxingDecodeListener = zxingDecodeListener;

        // Start ourselves capturing previews and decoding.
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case R.id.restart_preview:
                restartPreviewAndDecode();
                break;
            case R.id.decode_succeeded:
                state = State.SUCCESS;
                Bundle bundle = message.getData();
                Bitmap barcode = null;
                float scaleFactor = 1.0f;
                if (bundle != null) {
                    byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
                    if (compressedBitmap != null) {
                        barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                        // Mutable copy:
                        barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    scaleFactor = bundle.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
                }
                zxingDecodeListener.handleDecode((Result) message.obj, barcode, scaleFactor);
                break;
            case R.id.decode_failed:
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW;
                zxingDecodeListener.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
                break;
            case R.id.return_scan_result:
                zxingDecodeListener.returnScanResult((Intent) message.obj);
                break;
            case R.id.launch_product_query:
                break;
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            zxingDecodeListener.cameraRestartPreviewAndDecode(decodeThread.getHandler(), R.id.decode);
//            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
//            activity.drawViewfinder();
        }
    }
}
