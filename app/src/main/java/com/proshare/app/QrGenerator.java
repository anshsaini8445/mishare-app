package com.proshare.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrGenerator {

    public static Bitmap createAutoQr(String ipAddress) {
        try {
            String data = "HYPERSHARE|" + ipAddress + "|" + Build.MODEL;
            BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 480, 480);
            Bitmap bmp = Bitmap.createBitmap(480, 480, Bitmap.Config.RGB_565);
            for (int x = 0; x < 480; x++) {
                for (int y = 0; y < 480; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
