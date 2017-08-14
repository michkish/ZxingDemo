package zxing.trustway.cn.ydjwzxing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import zxing.trustway.cn.ydjwzxing.util.BitmapUtil;
import zxing.trustway.cn.ydjwzxing.util.DecodeHandler;
import zxing.trustway.cn.ydjwzxing.util.ZxingUtil;

public class MainActivity extends Activity implements View.OnClickListener {
    Button btn_decode, btn_generate;

    private Intent intent;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_decode = findViewById(R.id.btn_decode);
        btn_decode.setOnClickListener(this);
        btn_generate = findViewById(R.id.btn_generate);
        btn_generate.setOnClickListener(this);

        intent = getIntent();
        context = this;


        File file = new File(BitmapUtil.picPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_decode:
                startActivityForResult(new Intent(context, DecodeActivity.class), 1);
                break;
            case R.id.btn_generate:
                startActivityForResult(new Intent(context, GenerateActivity.class), 2);
                break;
            default:
                break;
        }
    }
}
