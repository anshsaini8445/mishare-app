package com.proshare.app;

import android.graphics.Bitmap;
import android.os.Build;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.HashMap;
import java.util.Map;

public class QrGenerator {

    private static final Map<String, Bitmap> qrCache = new HashMap<>();

    // Creates QR for either Web URL or Full Wi-Fi Direct Auto-Connect Payload
    public static Bitmap createAutoQr(String payload) {
        try {
            String data = (payload.startsWith("http") || payload.startsWith("HYPERSHARE|"))
                    ? payload
                    : ("HYPERSHARE|DIRECT-" + Build.MODEL + "|hyper1234|" + payload + "|" + Build.MODEL);

            if (qrCache.containsKey(data)) {
                return qrCache.get(data);
            }

            int size = 460;
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

    public static String buildWifiQrPayload(String ssid, String password, String ip) {
        String cleanSsid = (ssid == null || ssid.isEmpty()) ? "Hotspot" : ssid;
        String cleanPass = (password == null || password.isEmpty()) ? "none" : password;
        return "HYPERSHARE|" + cleanSsid + "|" + cleanPass + "|" + ip + "|" + Build.MODEL;
    }

    public static void displayLiveQr(ImageView iv, String payload) {
        if (iv == null) return;
        iv.setAlpha(0f);
        iv.setScaleX(0.88f);
        iv.setScaleY(0.88f);
        iv.setImageBitmap(createAutoQr(payload));
        iv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).start();
    }
}
