package com.proshare.app;

import android.Manifest;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
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
    private TextView tvStatus, tvSpeed, tvRadarPopup, tvHotspotInfo;
    private Button btnModeSwitch;
    private ImageView qrView;
    private ProgressBar bar;
    private EditText etIp;
    private UiHelper.LiveSpeedGraphView speedGraph;
    private String currentModeName = "Wi-Fi Direct";

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
        engine.startWebServer();

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 1);

        String mode = getIntent().getStringExtra("MODE");
        if ("RECEIVE".equals(mode)) {
            setContentView(buildOceanBlueReceiverUi());
        } else {
            setContentView(buildSenderLaserRadarUi());
        }
    }

    // 1. OCEAN-BLUE RECEIVER SCREEN WITH INSTANT QR & FAST MODE SWITCH (Video 00:39 - 00:52)
    private View buildOceanBlueReceiverUi() {
        p2pEngine.createFastGroup();
        final String myIp = MultiTransferEngine.getLocalIp(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(UiHelper.gradientBento("#0284C7", "#2563EB", 0));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

        // Top Bar: ← Waiting for sender...  [ ⇄ Wi-Fi Direct ] (Video 00:39)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvBack = UiHelper.makeTitle(this, "←  Waiting for sender...", "#FFFFFF", 19f);
        tvBack.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvBack.setOnClickListener(v -> finish());

        btnModeSwitch = new Button(this);
        btnModeSwitch.setText("⇄ " + currentModeName);
        btnModeSwitch.setAllCaps(false);
        btnModeSwitch.setTextColor(Color.WHITE);
        btnModeSwitch.setBackground(UiHelper.bentoBox("#1D4ED8", "#93C5FD", 50, 2));
        btnModeSwitch.setOnClickListener(v -> showTransferModeDialog(myIp));

        topBar.addView(tvBack);
        topBar.addView(btnModeSwitch);
        root.addView(topBar);

        TextView stepInfo = new TextView(this);
        stepInfo.setText("① Open HyperShare on sender device.\n② Ask the sender to tap \"Send\" and scan QR Code to connect.");
        stepInfo.setTextColor(Color.parseColor("#E0F2FE"));
        stepInfo.setPadding(0, 30, 0, 25);
        root.addView(stepInfo);

        // Crisp White QR Card (Instant 0ms QR - No "Creating QR Code..." Lag!)
        LinearLayout whiteCard = new LinearLayout(this);
        whiteCard.setOrientation(LinearLayout.VERTICAL);
        whiteCard.setGravity(Gravity.CENTER_HORIZONTAL);
        whiteCard.setBackground(UiHelper.box("#FFFFFF", 36));
        whiteCard.setPadding(40, 40, 40, 40);

        whiteCard.addView(UiHelper.makeTitle(this, "👤  " + Build.MODEL, "#0F172A", 17f));

        qrView = new ImageView(this);
        qrView.setImageBitmap(QrGenerator.createAutoQr(myIp));
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(450, 450);
        qLp.topMargin = 15;
        qrView.setLayoutParams(qLp);
        whiteCard.addView(qrView);

        tvHotspotInfo = new TextView(this);
        tvHotspotInfo.setText("Hotspot: DIRECT-50-" + Build.MODEL + "\nPassword: hyper1234  •  IP: " + myIp);
        tvHotspotInfo.setTextColor(Color.parseColor("#0284C7"));
        tvHotspotInfo.setTypeface(null, Typeface.BOLD);
        tvHotspotInfo.setGravity(Gravity.CENTER);
        tvHotspotInfo.setPadding(0, 10, 0, 20);
        whiteCard.addView(tvHotspotInfo);

        // ⚡ Fast Mode Switch Inside Grey Pill (Video 00:39)
        Switch fastSwitch = new Switch(this);
        fastSwitch.setText("⚡ Fast mode (5GHz / 2.4GHz Auto Boost)    ");
        fastSwitch.setChecked(true);
        fastSwitch.setTextColor(Color.parseColor("#0F172A"));
        fastSwitch.setTypeface(null, Typeface.BOLD);
        fastSwitch.setBackground(UiHelper.box("#F1F5F9", 35));
        fastSwitch.setPadding(35, 25, 35, 25);
        fastSwitch.setOnCheckedChangeListener((b, checked) -> {
            // Instant QR update in 0ms without any lag!
            qrView.setImageBitmap(QrGenerator.createAutoQr(myIp));
            tvHotspotInfo.setText("Hotspot: " + (checked ? "DIRECT-5G-" : "DIRECT-24G-") + Build.MODEL + "\nPassword: hyper1234  •  IP: " + myIp);
        });
        whiteCard.addView(fastSwitch);
        root.addView(whiteCard);

        // Live Speedometer Card
        root.addView(buildLiveSpeedCard());

        TextView storageBottom = new TextView(this);
        storageBottom.setText(AppSession.getStorageText());
        storageBottom.setTextColor(Color.parseColor("#BAE6FD"));
        storageBottom.setGravity(Gravity.CENTER);
        storageBottom.setPadding(0, 35, 0, 10);
        root.addView(storageBottom);

        engine.broadcastReceiverRadar(myIp);
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare");
        engine.startReceiver(this, dir, callback("Receiving"));

        scroll.addView(root);
        return scroll;
    }

    // 2. SENDER LASER FRAME & WHITE RADAR BOTTOM SHEET (Video 00:10 - 00:13)
    private View buildSenderLaserRadarUi() {
        String myIp = MultiTransferEngine.getLocalIp(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0B0F19"));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Dark Top Section with QR Laser Frame (Video 00:10)
        LinearLayout topSection = new LinearLayout(this);
        topSection.setOrientation(LinearLayout.VERTICAL);
        topSection.setGravity(Gravity.CENTER_HORIZONTAL);
        topSection.setPadding(40, 40, 40, 45);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView tvBack = UiHelper.makeTitle(this, "←  Connect to Receiver...", "#FFFFFF", 19f);
        tvBack.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvBack.setOnClickListener(v -> finish());

        Button btnSendPc = new Button(this);
        btnSendPc.setText("Send to PC");
        btnSendPc.setAllCaps(false);
        btnSendPc.setTextColor(Color.parseColor("#0F172A"));
        btnSendPc.setBackground(UiHelper.box("#FFFFFF", 50));
        btnSendPc.setOnClickListener(v -> finish());

        header.addView(tvBack);
        header.addView(btnSendPc);
        topSection.addView(header);

        // Animated Green Laser QR Frame (Tap to Open Camera Scanner)
        UiHelper.QrLaserFrameView laserFrame = new UiHelper.QrLaserFrameView(this);
        LinearLayout.LayoutParams lfLp = new LinearLayout.LayoutParams(480, 480);
        lfLp.topMargin = 55;
        laserFrame.setLayoutParams(lfLp);
        laserFrame.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
        topSection.addView(laserFrame);

        TextView tvPlace = new TextView(this);
        tvPlace.setText("Tap Frame to Open Camera & Scan QR Code");
        tvPlace.setTextColor(Color.parseColor("#CBD5E1"));
        tvPlace.setPadding(0, 25, 0, 15);
        topSection.addView(tvPlace);

        Button btnOpenCam = UiHelper.makeBtn(this, "📷  OPEN CAMERA QR SCANNER", "#16CC3E", "#FFFFFF");
        btnOpenCam.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
        topSection.addView(btnOpenCam);
        root.addView(topSection);

        // White Rounded Bottom Sheet: "Searching for receiver..." + Green Radar (Video 00:10)
        LinearLayout bottomSheet = new LinearLayout(this);
        bottomSheet.setOrientation(LinearLayout.VERTICAL);
        bottomSheet.setGravity(Gravity.CENTER_HORIZONTAL);
        bottomSheet.setBackground(UiHelper.box("#FFFFFF", 55));
        bottomSheet.setPadding(45, 45, 45, 60);
        bottomSheet.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        TextView tvSearch = UiHelper.makeTitle(this, "Searching for receiver...", "#0F172A", 19f);
        tvSearch.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        bottomSheet.addView(tvSearch);

        UiHelper.RadarSweepView radar = new UiHelper.RadarSweepView(this);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(220, 220);
        rLp.topMargin = 35;
        radar.setLayoutParams(rLp);
        bottomSheet.addView(radar);

        tvRadarPopup = UiHelper.makeTitle(this, "Auto-Radar Active (2.4GHz & 5GHz)\nFor TV/PC Browser: http://" + myIp + ":8080", "#64748B", 13.5f);
        tvRadarPopup.setGravity(Gravity.CENTER);
        tvRadarPopup.setPadding(15, 25, 15, 25);
        bottomSheet.addView(tvRadarPopup);

        etIp = new EditText(this);
        etIp.setHint("Or Enter Receiver IP (e.g. 192.168.43.1)");
        etIp.setTextColor(Color.BLACK);
        etIp.setBackground(UiHelper.box("#F1F5F9", 30));
        etIp.setPadding(35, 25, 35, 25);
        etIp.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        bottomSheet.addView(etIp);

        Button manualBtn = UiHelper.makeBtn(this, "🚀  CONNECT & SEND NOW", "#1890FF", "#FFFFFF");
        manualBtn.setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            if (!ip.isEmpty()) startSend(ip);
        });
        bottomSheet.addView(manualBtn);

        bottomSheet.addView(buildLiveSpeedCard());
        root.addView(bottomSheet);

        startRadarListen();
        scroll.addView(root);
        return scroll;
    }

    // Transfer Mode Dialog: Wi-Fi Direct vs Hotspot (Video 00:48)
    private void showTransferModeDialog(String myIp) {
        String[] modes = {
                "⚡ Wi-Fi Direct (No internet consumed - Recommended)",
                "((📡)) Hotspot (Create a Hotspot to connect)"
        };
        new AlertDialog.Builder(this)
                .setTitle("Transfer mode")
                .setItems(modes, (d, which) -> {
                    currentModeName = (which == 0) ? "Wi-Fi Direct" : "Hotspot";
                    btnModeSwitch.setText("⇄ " + currentModeName);
                    qrView.setImageBitmap(QrGenerator.createAutoQr(myIp));
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private View buildLiveSpeedCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 35, 2));
        card.setPadding(35, 30, 35, 30);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 30;
        card.setLayoutParams(lp);

        tvSpeed = UiHelper.makeTitle(this, "⚡ 0.0 Mbps", "#10B981", 26f);
        card.addView(tvSpeed);

        speedGraph = new UiHelper.LiveSpeedGraphView(this);
        speedGraph.setLayoutParams(new LinearLayout.LayoutParams(-1, 120));
        card.addView(speedGraph);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        card.addView(bar);

        tvStatus = UiHelper.makeTitle(this, "Zero-Lag Turbo Socket Ready", "#334155", 13f);
        tvStatus.setPadding(0, 10, 0, 0);
        card.addView(tvStatus);
        return card;
    }

    private void startRadarListen() {
        engine.scanRadar(new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String name, String ip) {
                runOnUiThread(() -> {
                    tvRadarPopup.setText("🟢 RECEIVER FOUND: " + name + " (" + ip + ")\n👉 TAP HERE TO SEND INSTANTLY!");
                    tvRadarPopup.setTextColor(Color.parseColor("#16CC3E"));
                    tvRadarPopup.setOnClickListener(v -> startSend(ip));
                });
            }
            @Override public void onProgress(String f, int i, int t, int p, double s) {}
            @Override public void onDone(int t, long b) {}
            @Override public void onError(String m) {}
        });
    }

    private void startSend(String ip) {
        tvStatus.setText("⚡ Sending to " + ip + "...");
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
                    tvStatus.setText("✅ Transfer Complete: " + t + " files (" + AppSession.formatSize(b) + ")");
                });
            }
            @Override public void onError(String m) {
                runOnUiThread(() -> tvStatus.setText("⚠️ " + m));
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
