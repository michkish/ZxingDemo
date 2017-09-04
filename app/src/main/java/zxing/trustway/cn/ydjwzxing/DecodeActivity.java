package zxing.trustway.cn.ydjwzxing;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.util.Collection;
import java.util.Map;

import zxing.trustway.cn.ydjwzxing.listener.ZxingDecodeListener;
import zxing.trustway.cn.ydjwzxing.util.AutoFocusManager;
import zxing.trustway.cn.ydjwzxing.util.BeepManager;
import zxing.trustway.cn.ydjwzxing.util.BitmapUtil;
import zxing.trustway.cn.ydjwzxing.util.ContextDecodeHandler;
import zxing.trustway.cn.ydjwzxing.util.DecodeFormatManager;
import zxing.trustway.cn.ydjwzxing.util.DecodeHintManager;
import zxing.trustway.cn.ydjwzxing.util.Intents;
import zxing.trustway.cn.ydjwzxing.util.PreviewCallback;
import zxing.trustway.cn.ydjwzxing.widget.ViewfinderView;

/**
 * Created by Zheming.xin on 2017/8/14.
 */

public class DecodeActivity extends Activity implements SurfaceHolder.Callback, ZxingDecodeListener {
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
    private Context context;
    private Point bestPreviewSize;

    private Result savedResultToShow;

    private BeepManager beepManager;
    private PreviewCallback previewCallback;
    private AutoFocusManager autoFocusManager;
//    private IntentSource source;
    private boolean previewing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);
        intent = getIntent();
        context = this;

        sv = (SurfaceView) findViewById(R.id.surfaceview_zxing);
        vfv = (ViewfinderView) findViewById(R.id.vfv_zxing);
        decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        decodeHints = DecodeHintManager.parseDecodeHints(intent);
        characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        beepManager = new BeepManager(context);

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getApplication(),Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CAMERA} ,0x02);
            } else{
                doCamera();
            }
        } else {
            doCamera();
        }
    }

    public void doCamera() {
        sv.setVisibility(View.VISIBLE);
        mCamera = getCamera();
        holder = sv.getHolder();
        holder.addCallback(this);
        sv.setFocusable(true);
        sv.setFocusableInTouchMode(true);
        sv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
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
            decodeOrStoreSavedBitmap(null, null);
//            setCameraDisplayOrientation(this,c);
            c.startPreview();
            previewing = true;

            Camera.Parameters param = c.getParameters();
            param.setPictureFormat(ImageFormat.JPEG);
            bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(param, BitmapUtil.screenResolution);
            BitmapUtil.cameraResolution = bestPreviewSize;
            param.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            if (previewCallback == null) {
                previewCallback = new PreviewCallback(BitmapUtil.cameraResolution);
            }
            if (handler == null) {
                handler = new ContextDecodeHandler(decodeFormats, decodeHints, characterSet, vfv, this);
            }
            if (autoFocusManager == null) {
                autoFocusManager = new AutoFocusManager(context, c);
            } else {
                autoFocusManager.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            sv.setVisibility(View.GONE);
            if (autoFocusManager != null) {
                autoFocusManager.stop();
            }
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            previewing = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPreview(mCamera, surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

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

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    @Override
    public void handleDecode(Result result, Bitmap barcode, float scaleFactor) {
        lastResult = result;
        beepManager.playBeepSoundAndVibrate();
        Log.d("Zxing", "start handleDecode");
//        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, result);

        BitmapUtil.saveBitmap(barcode);

        boolean fromLiveScan = barcode != null;
        if (result != null && !TextUtils.isEmpty(result.getText())) {
            Log.d("Zxing", "zxing decode: " + result.getText());
        } else {

        }

        restartPreviewAfterDelay(2000);
//        if (fromLiveScan) {
//            // Then not from history, so beep/vibrate and we have an image to draw on
//
//            drawResultPoints(barcode, scaleFactor, result);
//        }
    }

    @Override
    public void requestPreviewFrame(Handler handler, int id) {
        if (previewing) {
            previewCallback.setHandler(handler, id);
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    }

    @Override
    public void returnScanResult(Intent intent) {
        this.setResult(Activity.RESULT_OK, intent);
        this.finish();
    }

    @Override
    public void cameraRestartPreviewAndDecode(Handler handler, int id) {
        if (previewing) {
            previewCallback.setHandler(handler, id);
            mCamera.setOneShotPreviewCallback(previewCallback);
            vfv.drawViewfinder();
        }
    }

    @Override
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = BitmapUtil.getFramingRectInPreview(BitmapUtil.screenResolution, BitmapUtil.cameraResolution);
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        Log.d("Guess", "width: " + width + " height: " + height);
        Log.d("Guess", "rect: " + rect);
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
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
