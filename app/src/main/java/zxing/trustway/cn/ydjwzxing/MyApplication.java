package zxing.trustway.cn.ydjwzxing;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import zxing.trustway.cn.ydjwzxing.util.BitmapUtil;

/**
 * Created by Zheming.xin on 2017/9/4.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        BitmapUtil.screenResolution = new Point();
        WindowManager wm = (WindowManager) (getSystemService(Context.WINDOW_SERVICE));
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        BitmapUtil.screenResolution.x = dm.widthPixels;
        BitmapUtil.screenResolution.y = dm.heightPixels;
    }
}
