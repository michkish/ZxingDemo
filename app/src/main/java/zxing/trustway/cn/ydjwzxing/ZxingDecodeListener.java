package zxing.trustway.cn.ydjwzxing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;

/**
 * Created by Zheming.xin on 2017/8/10.
 */

public interface ZxingDecodeListener {
    void handleDecode(Result result, Bitmap barcode, float scaleFactor);
    void requestPreviewFrame(Handler handler, int id);
    void returnScanResult(Intent intent);
    void cameraRestartPreviewAndDecode(Handler handler, int id);
    PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height);
    void decodeSucceeded(Bundle bundle, int id, Result rawResult);
    void decodeFailed(int id);
}
