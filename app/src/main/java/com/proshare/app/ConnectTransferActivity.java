package com.proshare.app;

import android.Manifest;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private FrameLayout rootHolder;
    private MultiTransferEngine engine;
    private WifiDirectEngine p2pEngine;

    private TextView tvStatus, tvSpeed, tvRadarPopup, tvHotspotDetails, tvFileCounter;
    private Button btnPauseResume;
    private ImageView qrView;
    private EditText etIp;
    private UiHelper.LiquidWaveSphereView progressCircle;
    private boolean transferScreenShown = false;

    private final ActivityResultLauncher<ScanOptions> scanner =
            registerForActivityResult(new ScanContract(), res -> {
                if (res.getContents() != null) {
                    handleScannedQrPayload(res.getContents().trim());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engine = new MultiTransferEngine();
        p2pEngine = new WifiDirectEngine();
        engine.startWebServer();

        requestConnectionPermissions();

        rootHolder = new FrameLayout(this);
        setContentView(rootHolder);

        String mode = getIntent().getStringExtra("MODE");
        if ("RECEIVE".equals(mode)) {
            rootHolder.addView(buildCleanReceiverQrScreen());
        } else {
            rootHolder.addView(buildCleanSenderScannerScreen());
        }
    }

    private void requestConnectionPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        }
    }

    // 1. CLEAN INSHARE-STYLE RECEIVER QR SCREEN (No Cluttered 0% Box at Bottom!)
    private View buildCleanReceiverQrScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(UiHelper.gradientBento("#0284C7", "#2563EB", 0));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        // Top Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvBack = UiHelper.makeTitle(this, "←  Waiting for sender...", "#FFFFFF", 20f);
        tvBack.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvBack.setOnClickListener(v -> finish());

        TextView bandBadge = new TextView(this);
        bandBadge.setText("  ⚡ " + WifiDirectEngine.getBestSupportedBandName(this) + "  ");
        bandBadge.setTextColor(Color.WHITE);
        bandBadge.setTextSize(12f);
        bandBadge.setTypeface(null, Typeface.BOLD);
        bandBadge.setBackground(UiHelper.bentoBox("#1D4ED8", "#93C5FD", 45, 2));
        bandBadge.setPadding(22, 14, 22, 14);

        topBar.addView(tvBack);
        topBar.addView(bandBadge);
        root.addView(topBar);

        TextView steps = new TextView(this);
        steps.setText("① Open HyperShare on sender device.\n② Tap \"Send\" and scan this QR Code for automatic high-speed connection.");
        steps.setTextColor(Color.parseColor("#E0F2FE"));
        steps.setTextSize(14f);
        steps.setPadding(0, 35, 0, 30);
        root.addView(steps);

        // Clean White QR Card (Like InShare 00:44)
        LinearLayout whiteCard = new LinearLayout(this);
        whiteCard.setOrientation(LinearLayout.VERTICAL);
        whiteCard.setGravity(Gravity.CENTER_HORIZONTAL);
        whiteCard.setBackground(UiHelper.box("#FFFFFF", 40));
        whiteCard.setPadding(45, 45, 45, 45);

        whiteCard.addView(UiHelper.makeTitle(this, "👤  " + Build.MODEL, "#0F172A", 18f));

        qrView = new ImageView(this);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(480, 480);
        qLp.topMargin = 20;
        qrView.setLayoutParams(qLp);
        whiteCard.addView(qrView);

        // Immediately show initial QR, then update with real 6GHz/5GHz/2.4GHz Group credentials
        final String initialIp = WifiDirectEngine.getRealDeviceIp(this);
        QrGenerator.displayLiveQr(qrView, QrGenerator.buildWifiQrPayload("Hotspot-" + Build.MODEL, "none", initialIp));

        tvHotspotDetails = new TextView(this);
        tvHotspotDetails.setText("Initializing " + WifiDirectEngine.getBestSupportedBandName(this) + "...\nIP: " + initialIp);
        tvHotspotDetails.setTextColor(Color.parseColor("#0284C7"));
        tvHotspotDetails.setTypeface(null, Typeface.BOLD);
        tvHotspotDetails.setGravity(Gravity.CENTER);
        tvHotspotDetails.setPadding(10, 15, 10, 20);
        whiteCard.addView(tvHotspotDetails);

        Switch fastSwitch = new Switch(this);
        fastSwitch.setText("⚡ Auto-Detect 6GHz / 5GHz / 2.4GHz   ");
        fastSwitch.setChecked(true);
        fastSwitch.setTextColor(Color.parseColor("#0F172A"));
        fastSwitch.setTypeface(null, Typeface.BOLD);
        fastSwitch.setBackground(UiHelper.box("#F1F5F9", 35));
        fastSwitch.setPadding(35, 22, 35, 22);
        whiteCard.addView(fastSwitch);

        root.addView(whiteCard);

        TextView tvStore = new TextView(this);
        tvStore.setText(AppSession.getStorageText());
        tvStore.setTextColor(Color.parseColor("#BAE6FD"));
        tvStore.setGravity(Gravity.CENTER);
        tvStore.setPadding(0, 45, 0, 15);
        root.addView(tvStore);

        // Start Real Hardware Group (6GHz -> 5GHz -> 2.4GHz) & Receiver Socket
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare");
        engine.startReceiver(this, dir, transferCallback("Receiving"));

        p2pEngine.startReceiverGroup(this, (ssid, password, ownerIp, bandLabel) -> runOnUiThread(() -> {
            String fullPayload = QrGenerator.buildWifiQrPayload(ssid, password, ownerIp);
            QrGenerator.displayLiveQr(qrView, fullPayload);
            tvHotspotDetails.setText("Network: " + ssid + "\nPassword: " + password + "  •  IP: " + ownerIp);
            engine.broadcastReceiverRadar(ownerIp, ssid, password);
        }));

        scroll.addView(root);
        return scroll;
    }

    // 2. CLEAN INSHARE-STYLE SENDER SCANNER + WHITE RADAR BOTTOM SHEET (Like Video 00:10)
    private View buildCleanSenderScannerScreen() {
        p2pEngine.discoverNearbyPeers(this);

        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setBackgroundColor(Color.parseColor("#0B0F19"));

        // Top Dark Camera Frame Section
        LinearLayout topSection = new LinearLayout(this);
        topSection.setOrientation(LinearLayout.VERTICAL);
        topSection.setGravity(Gravity.CENTER_HORIZONTAL);
        topSection.setPadding(40, 40, 40, 35);
        topSection.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.15f));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView tvBack = UiHelper.makeTitle(this, "←  Connect to Receiver...", "#FFFFFF", 19f);
        tvBack.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvBack.setOnClickListener(v -> finish());
        header.addView(tvBack);
        topSection.addView(header);

        UiHelper.QrLaserFrameView laserFrame = new UiHelper.QrLaserFrameView(this);
        LinearLayout.LayoutParams lfLp = new LinearLayout.LayoutParams(420, 420);
        lfLp.topMargin = 35;
        laserFrame.setLayoutParams(lfLp);
        laserFrame.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
        topSection.addView(laserFrame);

        TextView tvHint = new TextView(this);
        tvHint.setText("Place the QR Code in the frame");
        tvHint.setTextColor(Color.parseColor("#CBD5E1"));
        tvHint.setPadding(0, 18, 0, 14);
        topSection.addView(tvHint);

        Button btnScan = UiHelper.makeBtn(this, "📷  SCAN RECEIVER QR CODE", "#16CC3E", "#FFFFFF");
        btnScan.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
        topSection.addView(btnScan);
        master.addView(topSection);

        // Clean White Radar Bottom Sheet (Like InShare 00:10)
        ScrollView bottomScroll = new ScrollView(this);
        bottomScroll.setBackground(UiHelper.box("#FFFFFF", 50));
        bottomScroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 0.95f));

        LinearLayout bottomSheet = new LinearLayout(this);
        bottomSheet.setOrientation(LinearLayout.VERTICAL);
        bottomSheet.setGravity(Gravity.CENTER_HORIZONTAL);
        bottomSheet.setPadding(45, 40, 45, 45);

        TextView tvSearch = UiHelper.makeTitle(this, "Searching for receiver...", "#0F172A", 18.5f);
        tvSearch.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        bottomSheet.addView(tvSearch);

        UiHelper.RadarSweepView radar = new UiHelper.RadarSweepView(this);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(170, 170);
        rLp.topMargin = 24;
        radar.setLayoutParams(rLp);
        bottomSheet.addView(radar);

        tvRadarPopup = UiHelper.makeTitle(this, "Auto-detecting nearby 6GHz / 5GHz / 2.4GHz receiver...", "#64748B", 13.5f);
        tvRadarPopup.setGravity(Gravity.CENTER);
        tvRadarPopup.setPadding(12, 18, 12, 18);
        bottomSheet.addView(tvRadarPopup);

        // Compact Manual IP Fallback Row
        LinearLayout ipRow = new LinearLayout(this);
        ipRow.setOrientation(LinearLayout.HORIZONTAL);
        ipRow.setGravity(Gravity.CENTER_VERTICAL);
        ipRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        etIp = new EditText(this);
        etIp.setHint("Receiver IP (e.g. 192.168.49.1)");
        etIp.setTextSize(13.5f);
        etIp.setTextColor(Color.BLACK);
        etIp.setHintTextColor(Color.parseColor("#94A3B8"));
        etIp.setBackground(UiHelper.box("#F1F5F9", 30));
        etIp.setPadding(30, 22, 30, 22);
        etIp.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnGo = new Button(this);
        btnGo.setText("Connect");
        btnGo.setAllCaps(false);
        btnGo.setTextColor(Color.WHITE);
        btnGo.setBackground(UiHelper.box("#1890FF", 30));
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(230, 105);
        gLp.leftMargin = 16;
        btnGo.setLayoutParams(gLp);
        btnGo.setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            if (!ip.isEmpty()) connectAndSend("Hotspot", "none", ip);
        });

        ipRow.addView(etIp);
        ipRow.addView(btnGo);
        bottomSheet.addView(ipRow);

        bottomScroll.addView(bottomSheet);
        master.addView(bottomScroll);

        startRadarDiscovery();
        return master;
    }

    // 3. DEDICATED ACTIVE TRANSFER SCREEN (Opens automatically once connection starts!)
    private void showDedicatedTransferScreen(String initialState) {
        if (transferScreenShown) return;
        transferScreenShown = true;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(45, 50, 45, 45);

        LinearLayout header = new LinearLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView back = UiHelper.makeTitle(this, "←  High-Speed Transfer", "#0F172A", 20f);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        root.addView(header);

        tvSpeed = UiHelper.makeTitle(this, "⚡ 0.0 Mbps", "#16CC3E", 30f);
        tvSpeed.setPadding(0, 45, 0, 15);
        root.addView(tvSpeed);

        progressCircle = new UiHelper.LiquidWaveSphereView(this);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(420, 420);
        cLp.topMargin = 20;
        cLp.bottomMargin = 25;
        progressCircle.setLayoutParams(cLp);
        root.addView(progressCircle);

        tvFileCounter = UiHelper.makeTitle(this, initialState, "#0F172A", 16f);
        tvFileCounter.setGravity(Gravity.CENTER);
        root.addView(tvFileCounter);

        tvStatus = new TextView(this);
        tvStatus.setText("Band: " + WifiDirectEngine.getBestSupportedBandName(this));
        tvStatus.setTextColor(Color.parseColor("#64748B"));
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(20, 12, 20, 35);
        root.addView(tvStatus);

        btnPauseResume = UiHelper.makeBtn(this, "⏸  PAUSE TRANSFER", "#F1F5F9", "#0F172A");
        btnPauseResume.setOnClickListener(v -> {
            engine.togglePause();
            btnPauseResume.setText(engine.isPaused() ? "▶  RESUME TRANSFER" : "⏸  PAUSE TRANSFER");
        });
        root.addView(btnPauseResume);

        scroll.addView(root);
        rootHolder.removeAllViews();
        rootHolder.addView(scroll);
    }

    private void handleScannedQrPayload(String raw) {
        // Format: HYPERSHARE|ssid|pass|ip|model
        String[] parts = raw.split("\\|");
        if (parts.length >= 4 && "HYPERSHARE".equals(parts[0])) {
            connectAndSend(parts[1], parts[2], parts[3]);
        } else if (parts.length >= 2) {
            connectAndSend("Hotspot", "none", parts[1]);
        } else {
            connectAndSend("Hotspot", "none", raw);
        }
    }

    private void startRadarDiscovery() {
        engine.scanRadar(new MultiTransferEngine.Callback() {
            @Override
            public void onRadarFound(final String model, final String ip, final String ssid, final String pass) {
                runOnUiThread(() -> {
                    if (tvRadarPopup != null) {
                        tvRadarPopup.setText("🟢 Found Receiver: " + model + " (" + ip + ")\n👉 TAP HERE TO CONNECT & SEND!");
                        tvRadarPopup.setTextColor(Color.parseColor("#16CC3E"));
                        tvRadarPopup.setOnClickListener(v -> connectAndSend(ssid, pass, ip));
                    }
                });
            }
            @Override public void onProgress(String n, int c, int t, int p, double s, long r) {}
            @Override public void onDone(int t, long b) {}
            @Override public void onError(String e) {}
        });
    }

    private void connectAndSend(String ssid, String pass, final String targetIp) {
        showDedicatedTransferScreen("Connecting to Receiver (" + targetIp + ")...");
        p2pEngine.autoConnectToReceiverWifi(this, ssid, pass, targetIp, new WifiDirectEngine.PeerConnectListener() {
            @Override
            public void onConnectedToReceiver(String receiverIp) {
                runOnUiThread(() -> tvStatus.setText("Connected! Starting high-speed stream..."));
                engine.sendFiles(ConnectTransferActivity.this, receiverIp, AppSession.selectedQueue, transferCallback("Sending"));
            }

            @Override
            public void onStatusUpdate(final String status) {
                runOnUiThread(() -> {
                    if (tvStatus != null) tvStatus.setText(status);
                });
            }
        });
    }

    private MultiTransferEngine.Callback transferCallback(final String tag) {
        return new MultiTransferEngine.Callback() {
            @Override
            public void onRadarFound(String m, String ip, String s, String p) {}

            @Override
            public void onProgress(final String name, final int current, final int total, final int pct, final double mbps, final long remBytes) {
                runOnUiThread(() -> {
                    if (!transferScreenShown) {
                        showDedicatedTransferScreen(tag + " Files...");
                    }
                    progressCircle.updateTransferState(pct, mbps, remBytes);
                    tvSpeed.setText(String.format(Locale.US, "⚡ %.1f Mbps", mbps));
                    tvFileCounter.setText(tag + " (" + current + " of " + total + ")");
                    tvStatus.setText(name);
                });
            }

            @Override
            public void onDone(final int totalFiles, final long totalBytes) {
                runOnUiThread(() -> {
                    if (!transferScreenShown) {
                        showDedicatedTransferScreen("Transfer Complete");
                    }
                    progressCircle.updateTransferState(100, 0, 0);
                    tvFileCounter.setText("✅ Successfully Transferred " + totalFiles + " Files");
                    tvStatus.setText("Total Size: " + AppSession.formatSize(totalBytes) + "  •  Saved in /Download/HyperShare");
                });
            }

            @Override
            public void onError(final String err) {
                runOnUiThread(() -> {
                    if (tvStatus != null) {
                        tvStatus.setText("⚠️ " + err);
                    }
                });
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.stop();
        p2pEngine.stopP2p(this);
    }
}
