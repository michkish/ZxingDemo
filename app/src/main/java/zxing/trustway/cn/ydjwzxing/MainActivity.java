package zxing.trustway.cn.ydjwzxing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;

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

    Intent intent;

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

    }

    @Override
    public void requestPreviewFrame(Handler handler, int id) {

    }

    @Override
    public void returnScanResult(Intent intent) {

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
        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
        message.setData(bundle);
        message.sendToTarget();
    }

    @Override
    public void decodeFailed(int id) {

    }
}
