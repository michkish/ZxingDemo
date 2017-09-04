package zxing.trustway.cn.ydjwzxing.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

/**
 * Created by Zheming.xin on 2017/8/14.
 */

public class ZxingUtil {

    private static Bitmap getScaleLogo(Bitmap logo, int w, int h) {
        if(logo == null) {
            return null;
        }

        Matrix matrix = new Matrix();
        float scaleFactor = Math.min((float)w * 1.0F / 5.0F / (float)logo.getWidth(), (float)h * 1.0F / 5.0F / (float)logo.getHeight());
        matrix.postScale(scaleFactor, scaleFactor);
        Bitmap result = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, true);
        return result;
    }

    public static Bitmap createImage(String text, int w, int h, Bitmap logo) {
        if(TextUtils.isEmpty(text)) {
            return null;
        }
        try {
            Bitmap e = getScaleLogo(logo, w, h);
            int offsetX = w / 2;
            int offsetY = h / 2;
            int scaleWidth = 0;
            int scaleHeight = 0;
            if(e != null) {
                scaleWidth = e.getWidth();
                scaleHeight = e.getHeight();
                offsetX = (w - scaleWidth) / 2;
                offsetY = (h - scaleHeight) / 2;
            }

            Hashtable hints = new Hashtable();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, Integer.valueOf(0));
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, w, h, hints);
            int[] pixels = new int[w * h];

            for(int bitmap = 0; bitmap < h; ++bitmap) {
                for(int x = 0; x < w; ++x) {
                    if(x >= offsetX && x < offsetX + scaleWidth && bitmap >= offsetY && bitmap < offsetY + scaleHeight) {
                        int pixel = e.getPixel(x - offsetX, bitmap - offsetY);
                        if(pixel == 0) {
                            if(bitMatrix.get(x, bitmap)) {
                                pixel = -16777216;
                            } else {
                                pixel = -1;
                            }
                        }

                        pixels[bitmap * w + x] = pixel;
                    } else if(bitMatrix.get(x, bitmap)) {
                        pixels[bitmap * w + x] = -16777216;
                    } else {
                        pixels[bitmap * w + x] = -1;
                    }
                }
            }

            Bitmap var16 = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            var16.setPixels(pixels, 0, w, 0, 0, w, h);
            return var16;
        } catch (WriterException var15) {
            var15.printStackTrace();
            return null;
        }
    }
}
