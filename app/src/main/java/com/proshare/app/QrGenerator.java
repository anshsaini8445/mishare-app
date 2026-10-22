package com.proshare.app;

import android.graphics.Bitmap;
import android.os.Build;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.HashMap;
import java.util.Map;

public class QrGenerator {

    private static final Map<String, Bitmap> qrCache = new HashMap<>();

    public static Bitmap createAutoQr(String payload) {
        try {
            String data = payload.contains("http") ? payload : ("HYPERSHARE|" + payload + "|" + Build.MODEL);
            if (qrCache.containsKey(data)) {
                return qrCache.get(data);
            }

            int size = 440;
            BitMatrix matrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size);
            int[] pixels = new int[size * size];

            for (int y = 0; y < size; y++) {
                int offset = y * size;
                for (int x = 0; x < size; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? 0xFF0F172A : 0xFFFFFFFF;
                }
            }

            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bmp.setPixels(pixels, 0, size, 0, 0, size, size);
            qrCache.put(data, bmp);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
