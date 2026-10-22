package com.proshare.app;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.File;
import java.util.Locale;

public class ConnectTransferActivity extends AppCompatActivity {

    private MultiTransferEngine engine;
    private WifiDirectEngine p2pEngine;
    private TextView tvStatus, tvSpeed, tvRadarPopup;
    private ProgressBar bar;
    private EditText etIp;
    private UiHelper.LiveSpeedGraphView speedGraph;

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), res -> {
                if (res.getContents() != null) {
                    String[] p = res.getContents().split("\\|");
                    startSend(p.length >= 2 ? p[1] : res.getContents().trim());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engine = new MultiTransferEngine();
        p2pEngine = new WifiDirectEngine();
        p2pEngine.initFastP2p(this);
        engine.startWebServer(); // Also activates TV / PC / Laptop Browser Download simultaneously!

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 1);

        String mode = getIntent().getStringExtra("MODE");
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#070B14"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 40, 35, 40);

        root.addView(UiHelper.makeTitle(this, "RECEIVE".equals(mode) ? "📡 2.4GHz / 5GHz Receiver Active" : "⚡ Auto-Radar (Phone • TV • PC)", "#00F5D4", 20f));
        root.addView(PermissionBox.build(this));

        String myIp = MultiTransferEngine.getLocalIp(this);

        if ("RECEIVE".equals(mode)) {
            p2pEngine.createFastGroup();

            LinearLayout qrBento = new LinearLayout(this);
            qrBento.setOrientation(LinearLayout.VERTICAL);
            qrBento.setGravity(Gravity.CENTER);
            qrBento.setBackground(UiHelper.bentoBox("#FFFFFF", "#00F5D4", 38, 6));
            qrBento.setPadding(30, 30, 30, 30);
            LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(-1, -2);
            qLp.topMargin = 25;
            qrBento.setLayoutParams(qLp);

            qrBento.addView(UiHelper.makeTitle(this, Build.MODEL + " • IP: " + myIp, "#070B14", 16f));
            TextView bandInfo = new TextView(this);
            bandInfo.setText(p2pEngine.getBandStatus(this));
            bandInfo.setTextColor(Color.parseColor("#2563EB"));
            qrBento.addView(bandInfo);

            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(QrGenerator.createAutoQr(myIp));
            qrBento.addView(qrView);
            root.addView(qrBento);

            engine.broadcastReceiverRadar(myIp);
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare");
            engine.startReceiver(this, dir, callback("Receiving"));
        } else {
            LinearLayout radarBento = new LinearLayout(this);
            radarBento.setOrientation(LinearLayout.VERTICAL);
            radarBento.setGravity(Gravity.CENTER);
            radarBento.setBackground(UiHelper.bentoBox("#11192E", "#38BDF8", 38, 2));
            radarBento.setPadding(30, 30, 30, 30);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
            rLp.topMargin = 25;
            radarBento.setLayoutParams(rLp);

            UiHelper.RadarSweepView radarView = new UiHelper.RadarSweepView(this);
            radarView.setLayoutParams(new LinearLayout.LayoutParams(320, 320));
            radarBento.addView(radarView);

            tvRadarPopup = UiHelper.makeTitle(this, "🔍 Scanning 2.4GHz & 5GHz Devices...\n📺 For LED TV / PC: Open http://" + myIp + ":8080", "#38BDF8", 13.5f);
            tvRadarPopup.setPadding(15, 20, 15, 10);
            radarBento.addView(tvRadarPopup);
            root.addView(radarBento);

            Button scanBtn = UiHelper.makeBtn(this, "📷 Scan Receiver QR Code", "#1E293B", "#00F5D4");
            scanBtn.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
            root.addView(scanBtn);

            etIp = new EditText(this);
            etIp.setHint("Or Enter Receiver IP (e.g. 192.168.43.1)");
            etIp.setHintTextColor(Color.GRAY);
            etIp.setTextColor(Color.WHITE);
            root.addView(etIp);

            Button manualBtn = UiHelper.makeBtn(this, "🚀 CONNECT & SEND (2.4G / 5G)", "#2563EB", "#FFFFFF");
            manualBtn.setOnClickListener(v -> {
                String ip = etIp.getText().toString().trim();
                if (!ip.isEmpty()) startSend(ip);
            });
            root.addView(manualBtn);
            startRadarListen();
        }

        LinearLayout speedBento = new LinearLayout(this);
        speedBento.setOrientation(LinearLayout.VERTICAL);
        speedBento.setBackground(UiHelper.bentoBox("#11192E", "#00F5D4", 38, 2));
        speedBento.setPadding(35, 30, 35, 30);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, -2);
        sLp.topMargin = 30;
        speedBento.setLayoutParams(sLp);

        tvSpeed = UiHelper.makeTitle(this, "⚡ 0.0 Mbps", "#00F5D4", 28f);
        speedBento.addView(tvSpeed);

        speedGraph = new UiHelper.LiveSpeedGraphView(this);
        speedGraph.setLayoutParams(new LinearLayout.LayoutParams(-1, 150));
        speedBento.addView(speedGraph);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        speedBento.addView(bar);

        tvStatus = UiHelper.makeTitle(this, "Ready • Works on 2.4GHz, 5GHz & Wi-Fi 6", "#CBD5E1", 13f);
        tvStatus.setPadding(0, 12, 0, 0);
        speedBento.addView(tvStatus);

        root.addView(speedBento);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void startRadarListen() {
        engine.scanRadar(new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String name, String ip) {
                runOnUiThread(() -> {
                    tvRadarPopup.setText("🟢 DEVICE FOUND: " + name + " (" + ip + ")\n👉 TAP HERE FOR ZERO-CLICK SEND!");
                    tvRadarPopup.setTextColor(Color.parseColor("#00F5D4"));
                    tvRadarPopup.setOnClickListener(v -> startSend(ip));
                });
            }
            @Override public void onProgress(String f, int i, int t, int p, double s) {}
            @Override public void onDone(int t, long b) {}
            @Override public void onError(String m) {}
        });
    }

    private void startSend(String ip) {
        tvStatus.setText("⚡ Connecting to " + ip + "...");
        engine.sendFiles(this, ip, AppSession.selectedQueue, callback("Sending"));
    }

    private MultiTransferEngine.Callback callback(String tag) {
        return new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String n, String ip) {}
            @Override public void onProgress(String f, int i, int t, int p, double s) {
                runOnUiThread(() -> {
                    bar.setProgress(p);
                    speedGraph.setSpeed(s);
                    tvSpeed.setText(String.format(Locale.US, "⚡ %.1f Mbps", s));
                    tvStatus.setText(tag + " (" + i + "/" + t + "): " + f + " [" + p + "%]");
                });
            }
            @Override public void onDone(int t, long b) {
                runOnUiThread(() -> {
                    bar.setProgress(100);
                    speedGraph.setSpeed(0);
                    tvStatus.setText("✅ Completed: " + t + " files (" + AppSession.formatSize(b) + ")");
                });
            }
            @Override public void onError(String m) {
                runOnUiThread(() -> tvStatus.setText("⚠️ Error: " + m));
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.stop();
        p2pEngine.stopP2p();
    }
}
