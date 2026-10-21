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
import android.widget.Switch;
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
    private TextView tvStatus, tvSpeed, tvRadar;
    private ProgressBar bar;
    private EditText etIp;

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

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 1);

        String mode = getIntent().getStringExtra("MODE");
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0B0F19"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

        root.addView(UiHelper.makeTitle(this, "RECEIVE".equals(mode) ? "📥 Waiting for Sender..." : "🚀 Connect to Receiver...", "#00F5D4", 22f));
        root.addView(PermissionBox.build(this));

        if ("RECEIVE".equals(mode)) {
            p2pEngine.createFastGroup();
            String myIp = MultiTransferEngine.getLocalIp(this);

            LinearLayout qrBox = new LinearLayout(this);
            qrBox.setOrientation(LinearLayout.VERTICAL);
            qrBox.setGravity(Gravity.CENTER);
            qrBox.setBackground(UiHelper.box("#FFFFFF", 35));
            qrBox.setPadding(30, 30, 30, 30);

            TextView modelTv = UiHelper.makeTitle(this, Build.MODEL + " (" + myIp + ")", "#0B0F19", 16f);
            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(QrGenerator.createAutoQr(myIp));

            Switch fastMode = new Switch(this);
            fastMode.setText("⚡ Fast Mode (5GHz / Wi-Fi 6)");
            fastMode.setChecked(true);
            fastMode.setTextColor(Color.BLACK);

            qrBox.addView(modelTv);
            qrBox.addView(qrView);
            qrBox.addView(fastMode);
            root.addView(qrBox);

            engine.broadcastReceiverRadar(myIp);
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare");
            engine.startReceiver(this, dir, callback("Receiving"));
        } else {
            Button scanBtn = UiHelper.makeBtn(this, "📷 SCAN RECEIVER QR CODE", "#00F5D4", "#0B0F19");
            scanBtn.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
            root.addView(scanBtn);

            tvRadar = UiHelper.makeTitle(this, "📡 Searching receiver on Radar...", "#38BDF8", 15f);
            tvRadar.setPadding(15, 25, 15, 25);
            root.addView(tvRadar);

            etIp = new EditText(this);
            etIp.setHint("Or Enter Receiver IP (e.g. 192.168.43.1)");
            etIp.setHintTextColor(Color.GRAY);
            etIp.setTextColor(Color.WHITE);
            root.addView(etIp);

            Button manualBtn = UiHelper.makeBtn(this, "🚀 CONNECT & SEND NOW", "#2563EB", "#FFFFFF");
            manualBtn.setOnClickListener(v -> {
                String ip = etIp.getText().toString().trim();
                if (!ip.isEmpty()) startSend(ip);
            });
            root.addView(manualBtn);
            startRadarListen();
        }

        tvSpeed = UiHelper.makeTitle(this, "⚡ 0.0 Mbps", "#FACC15", 28f);
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        tvStatus = UiHelper.makeTitle(this, "Engine Ready (Zero-Cancel Mode)", "#FFFFFF", 14f);

        root.addView(tvSpeed);
        root.addView(bar);
        root.addView(tvStatus);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void startRadarListen() {
        engine.scanRadar(new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String name, String ip) {
                runOnUiThread(() -> {
                    tvRadar.setText("🟢 Found: " + name + " (" + ip + ") [TAP TO SEND]");
                    tvRadar.setOnClickListener(v -> startSend(ip));
                });
            }
            @Override public void onProgress(String f, int i, int t, int p, double s) {}
            @Override public void onDone(int t, long b) {}
            @Override public void onError(String m) {}
        });
    }

    private void startSend(String ip) {
        tvStatus.setText("Connecting to " + ip + "...");
        engine.sendFiles(this, ip, AppSession.selectedQueue, callback("Sending"));
    }

    private MultiTransferEngine.Callback callback(String tag) {
        return new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String n, String ip) {}
            @Override public void onProgress(String f, int i, int t, int p, double s) {
                runOnUiThread(() -> {
                    bar.setProgress(p);
                    tvSpeed.setText(String.format(Locale.US, "⚡ %.1f Mbps", s));
                    tvStatus.setText(tag + " (" + i + "/" + t + "): " + f + " [" + p + "%]");
                });
            }
            @Override public void onDone(int t, long b) {
                runOnUiThread(() -> {
                    bar.setProgress(100);
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
