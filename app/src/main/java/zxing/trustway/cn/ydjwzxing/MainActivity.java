package zxing.trustway.cn.ydjwzxing;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import java.util.Collection;
import java.util.Map;

public class MainActivity extends Activity implements SurfaceHolder.Callback, ZxingDecodeListener {
    private SurfaceView sv;
    private ViewfinderView vfv;

    ContextDecodeHandler handler = null;

    private Camera mCamera;
    private SurfaceHolder holder;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,?> decodeHints;
    private String characterSet;

    private Intent intent;
    private Result lastResult;

    private IntentSource source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intent = getIntent();

        sv = (SurfaceView) findViewById(R.id.surfaceview_zxing);
        vfv = (ViewfinderView) findViewById(R.id.vfv_zxing);
        doCamera();
        decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        decodeHints = DecodeHintManager.parseDecodeHints(intent);
        characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
        handler = new ContextDecodeHandler(decodeFormats, decodeHints, characterSet, vfv, this);
    }

    public void doCamera() {
        sv.setVisibility(View.VISIBLE);
        mCamera = getCamera();
        holder = sv.getHolder();
        holder.addCallback(MainActivity.this);
        sv.setFocusable(true);
        sv.setFocusableInTouchMode(true);
        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Camera.Parameters param = mCamera.getParameters();
                    param.setPictureFormat(ImageFormat.JPEG);
                    param.setPreviewSize(800, 400);
                    param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    mCamera.autoFocus(null);
                } catch (Exception e) {
                }
            }
        });
    }

    private Camera getCamera()
    {
        try {
            return Camera.open();
        } catch (Exception e) {
            return null;
        }
    }

    private void startPreview(Camera c, SurfaceHolder h) {
        try {
            if (sv.getVisibility() == View.GONE) {
                sv.setVisibility(View.VISIBLE);
            }
            c.setPreviewDisplay(h);
            c.setDisplayOrientation(90);
//            setCameraDisplayOrientation(this,c);
            c.startPreview();

        } catch (Exception e) {
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            sv.setVisibility(View.GONE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPreview(mCamera, surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mCamera.stopPreview();
        startPreview(mCamera, surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera= getCamera();
            if (holder != null&&mCamera!=null) {
                startPreview(mCamera, holder);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    public void handleDecode(Result result, Bitmap barcode, float scaleFactor) {
        lastResult = result;
//        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, result);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
//            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, result);
        }

        switch (source) {
            case NATIVE_APP_INTENT:
            case PRODUCT_SEARCH_LINK:
//                handleDecodeExternally(rawResult, resultHandler, barcode);
                break;
            case ZXING_LINK:
//                if (scanFromWebPageManager == null || !scanFromWebPageManager.isScanFromWebPage()) {
//                    handleDecodeInternally(rawResult, resultHandler, barcode);
//                } else {
//                    handleDecodeExternally(rawResult, resultHandler, barcode);
//                }
                break;
            case NONE:
                break;
        }
    }

    @Override
    public void requestPreviewFrame(Handler handler, int id) {

    }

    @Override
    public void returnScanResult(Intent intent) {
        this.setResult(Activity.RESULT_OK, intent);
        this.finish();
    }

    @Override
    public void cameraRestartPreviewAndDecode(Handler handler, int id) {

    }

    @Override
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        return null;
    }

    @Override
    public void decodeSucceeded(Bundle bundle, int id, Result rawResult) {
        if (handler != null) {
            Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
            message.setData(bundle);
            message.sendToTarget();
        }
    }

    @Override
    public void decodeFailed(int id) {
        if (handler != null) {
            Message message = Message.obtain(handler, R.id.decode_failed);
            message.sendToTarget();
        }
    }

    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }
}
