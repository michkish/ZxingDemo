package zxing.trustway.cn.ydjwzxing.util;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Administrator on 2017/8/13.
 */

public final class PreviewCallback implements Camera.PreviewCallback {

    private static final String TAG = PreviewCallback.class.getSimpleName();

    private Handler previewHandler;
    private int previewMessage;
    private Point cameraResolution;

    public PreviewCallback(Point cameraResolution) {
        this.cameraResolution = cameraResolution;
    }

    public void setHandler(Handler previewHandler, int previewMessage) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (cameraResolution != null && previewHandler != null) {
//            BitmapUtil.compressImageFromPhotos(data);
            Message message = previewHandler.obtainMessage(previewMessage, cameraResolution.x,
                    cameraResolution.y, data);
            message.sendToTarget();
            previewHandler = null;
        } else {
            Log.d(TAG, "Got preview callback, but no handler or resolution available");
        }
    }
}
