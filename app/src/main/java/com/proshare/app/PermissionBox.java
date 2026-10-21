package com.proshare.app;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PermissionBox {

    public static LinearLayout build(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.box("#151E32", 30));
        card.setPadding(30, 25, 30, 25);

        card.addView(row(ctx, "📍 Open Location Service", new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        card.addView(row(ctx, "🦷 Bluetooth Fast Connect", new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)));
        card.addView(row(ctx, "📶 Open WLAN / Wi-Fi", new Intent(Settings.ACTION_WIFI_SETTINGS)));
        return card;
    }

    private static LinearLayout row(Context ctx, String name, Intent intent) {
        LinearLayout r = new LinearLayout(ctx);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, 12, 0, 12);

        TextView tv = new TextView(ctx);
        tv.setText(name);
        tv.setTextColor(Color.WHITE);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button b = new Button(ctx);
        b.setText("OPEN");
        b.setTextColor(Color.WHITE);
        b.setBackground(UiHelper.box("#10B981", 25));
        b.setOnClickListener(v -> ctx.startActivity(intent));

        r.addView(tv);
        r.addView(b);
        return r;
    }
}
