package com.proshare.app;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PermissionBox {

    public static LinearLayout build(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.bentoBox(ThemeEngine.card(ctx), ThemeEngine.accent(ctx), 30, 2));
        card.setPadding(30, 25, 30, 25);

        card.addView(row(ctx, "📍 Location (Required for 5GHz Radar)", new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        card.addView(row(ctx, "🦷 Bluetooth Fast Handshake", new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)));
        card.addView(row(ctx, "📶 WLAN / Hotspot High-Speed", new Intent(Settings.ACTION_WIFI_SETTINGS)));
        return card;
    }

    private static LinearLayout row(Context ctx, String name, Intent intent) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, 10, 0, 10);

        TextView tv = new TextView(ctx);
        tv.setText(name);
        tv.setTextColor(Color.parseColor(ThemeEngine.textMain(ctx)));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button b = new Button(ctx);
        b.setText("OPEN");
        b.setTextSize(11f);
        b.setTextColor(Color.WHITE);
        b.setBackground(UiHelper.box("#10B981", 25));
        b.setOnClickListener(v -> {
            HapticSoundEngine.tick(v);
            try { ctx.startActivity(intent); } catch (Exception ignored) {}
        });

        r.addView(tv);
        r.addView(b);
        return r;
    }

    public static void showFriendlyPermissionGuide(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("🛡️ Permission Needed for 2x Speed")
                .setMessage("To discover nearby phones on 5GHz Wi-Fi Direct and load your Videos/Apps without lag, please allow Storage & Location permissions in Settings.")
                .setPositiveButton("Open App Settings", (d, w) -> {
                    try {
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.setData(Uri.fromParts("package", ctx.getPackageName(), null));
                        ctx.startActivity(i);
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Later", null)
                .show();
    }
}
