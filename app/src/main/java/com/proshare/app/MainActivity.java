package com.proshare.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvSent, tvRec, tvFiles, tvStorage;
    private MultiTransferEngine webEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webEngine = new MultiTransferEngine();
        setContentView(buildHomeUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvSent != null) {
            tvSent.setText(AppSession.getSentText(this) + "\nSent");
            tvRec.setText(AppSession.getRecText(this) + "\nReceived");
            tvFiles.setText(AppSession.getFilesCount(this) + "\nFiles");
            tvStorage.setText(AppSession.getStorageText());
        }
    }

    private ScrollView buildHomeUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0B0F19"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 45, 40, 45);

        // Top Bar
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        Button menu = UiHelper.makeBtn(this, "☰ Menu", "#1E293B", "#FFFFFF");
        menu.setLayoutParams(new LinearLayout.LayoutParams(220, 120));
        menu.setOnClickListener(v -> showDrawer());

        TextView title = UiHelper.makeTitle(this, "  ⚡ HyperShare", "#00F5D4", 20f);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button invite = UiHelper.makeBtn(this, "⊕ Invite", "#2563EB", "#FFFFFF");
        invite.setLayoutParams(new LinearLayout.LayoutParams(220, 120));
        invite.setOnClickListener(v -> showInvite());

        top.addView(menu);
        top.addView(title);
        top.addView(invite);
        root.addView(top);

        // Stats Card (Screenshot 7)
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setBackground(UiHelper.box("#151E32", 36));
        stats.setPadding(35, 30, 35, 30);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, -2);
        sLp.topMargin = 40;
        stats.setLayoutParams(sLp);

        stats.addView(UiHelper.makeTitle(this, "📱 Device: " + Build.MODEL, "#FFFFFF", 15f));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 25, 0, 10);
        tvSent = statBox("0 KB\nSent", "#00F5D4");
        tvRec = statBox("0 KB\nReceived", "#38BDF8");
        tvFiles = statBox("0\nFiles", "#FACC15");
        row.addView(tvSent);
        row.addView(tvRec);
        row.addView(tvFiles);
        stats.addView(row);
        root.addView(stats);

        // Big Send & Receive Buttons (Screenshot 9)
        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(-1, -2);
        aLp.topMargin = 45;
        actRow.setLayoutParams(aLp);

        LinearLayout sendCard = bigCard("🚀\nSEND", "#10B981");
        sendCard.setOnClickListener(v -> startActivity(new Intent(this, FilePickerActivity.class)));

        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(30, 10));

        LinearLayout recCard = bigCard("📥\nRECEIVE", "#3B82F6");
        recCard.setOnClickListener(v -> {
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "RECEIVE");
            startActivity(i);
        });

        actRow.addView(sendCard);
        actRow.addView(sp);
        actRow.addView(recCard);
        root.addView(actRow);

        // Connect to PC / iOS / Jio Web Share (Screenshot 8 & 9)
        Button pcBtn = UiHelper.makeBtn(this, "💻 Connect to PC / iOS / Tablet (Web Share)", "#1E293B", "#38BDF8");
        pcBtn.setOnClickListener(v -> showWebShareDialog());
        root.addView(pcBtn);

        Button batteryBtn = UiHelper.makeBtn(this, "🛡️ Avoid Abnormal Stop (Battery Fix)", "#151E32", "#A78BFA");
        batteryBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        root.addView(batteryBtn);

        tvStorage = UiHelper.makeTitle(this, AppSession.getStorageText(), "#94A3B8", 13f);
        tvStorage.setGravity(Gravity.CENTER);
        tvStorage.setPadding(0, 50, 0, 10);
        root.addView(tvStorage);

        scroll.addView(root);
        return scroll;
    }

    private TextView statBox(String text, String color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        return tv;
    }

    private LinearLayout bigCard(String text, String color) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER);
        c.setBackground(UiHelper.box(color, 40));
        c.setPadding(30, 60, 30, 60);
        c.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tv = UiHelper.makeTitle(this, text, "#0B0F19", 20f);
        tv.setGravity(Gravity.CENTER);
        c.addView(tv);
        return c;
    }

    private void showWebShareDialog() {
        webEngine.startWebServer();
        String url = "http://" + MultiTransferEngine.getLocalIp(this) + ":" + MultiTransferEngine.WEB_PORT;
        new AlertDialog.Builder(this)
                .setTitle("💻 Web Share Active")
                .setMessage("Open browser on PC / iOS / JioPhone and enter:\n\n" + url)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showInvite() {
        String[] opts = {"📶 Hotspot Invite", "🦷 Bluetooth Share", "💬 Share Link on WhatsApp"};
        new AlertDialog.Builder(this)
                .setTitle("Invite to Install (No Internet Needed)")
                .setItems(opts, (d, w) -> {
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_TEXT, "Download HyperShare Pro for 150+ Mbps Ultra-Fast File Transfer!");
                    startActivity(Intent.createChooser(s, "Share via"));
                })
                .show();
    }

    private void showDrawer() {
        String[] menu = {"🌐 Web Share (Connect to PC)", "🛡️ Avoid Abnormal Stop (Battery Fix)", "⊕ Invite Friends"};
        new AlertDialog.Builder(this)
                .setTitle("HyperShare Menu (" + Build.MODEL + ")")
                .setItems(menu, (d, w) -> {
                    if (w == 0) showWebShareDialog();
                    else if (w == 1) startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    else showInvite();
                })
                .show();
    }
}
