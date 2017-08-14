package zxing.trustway.cn.ydjwzxing.listener;

import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

import zxing.trustway.cn.ydjwzxing.widget.ViewfinderView;

/**
 * Created by Zheming.xin on 2017/8/10.
 */

public final class ViewfinderResultPointCallback implements ResultPointCallback {

    private final ViewfinderView viewfinderView;

    public ViewfinderResultPointCallback(ViewfinderView viewfinderView) {
        this.viewfinderView = viewfinderView;
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {
        viewfinderView.addPossibleResultPoint(point);
    }

}
