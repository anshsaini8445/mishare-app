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
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConnectTransferActivity extends AppCompatActivity {

    private MultiTransferEngine engine;
    private WifiDirectEngine p2pEngine;
    private TextView tvStatus, tvSpeed, tvRadarPopup, tvHotspotInfo, tvChatLog;
    private Button btnModeSwitch, btnPauseResume;
    private ImageView qrView;
    private EditText etIp;
    private UiHelper.LiquidWaveSphereView liquidSphere;
    private String currentModeName = "Wi-Fi Direct";
    private String connectedPeerIp = "192.168.43.1";

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

        // Start Offline Chat Listener
        engine.startOfflineChatServer((sender, msg) -> runOnUiThread(() -> {
            HapticSoundEngine.popSuccess(this);
            if (tvChatLog != null) {
                tvChatLog.setText("💬 " + sender + ": " + msg);
            }
            Toast.makeText(this, "💬 " + sender + ": " + msg, Toast.LENGTH_LONG).show();
        }));

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 1);

        String mode = getIntent().getStringExtra("MODE");
        View content = "RECEIVE".equals(mode) ? buildReceiverUi() : buildSenderUi();
        setContentView(ThemeEngine.applyOneHandWrapper(this, content));
    }

    // 1. RECEIVER UI WITH LIVE SHIMMER QR & 3D LIQUID WAVE SPHERE
    private View buildReceiverUi() {
        p2pEngine.createFastGroup();
        final String myIp = MultiTransferEngine.getLocalIp(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(UiHelper.gradientBento("#0284C7", "#1E3A8A", 0));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

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

        // Live QR Card
        LinearLayout whiteCard = new LinearLayout(this);
        whiteCard.setOrientation(LinearLayout.VERTICAL);
        whiteCard.setGravity(Gravity.CENTER_HORIZONTAL);
        whiteCard.setBackground(UiHelper.box("#FFFFFF", 38));
        whiteCard.setPadding(35, 35, 35, 35);
        LinearLayout.LayoutParams wcLp = new LinearLayout.LayoutParams(-1, -2);
        wcLp.topMargin = 25;
        whiteCard.setLayoutParams(wcLp);

        whiteCard.addView(UiHelper.makeTitle(this, "👤 " + Build.MODEL + " (" + myIp + ")", "#0F172A", 16.5f));

        qrView = new ImageView(this);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(420, 420);
        qLp.topMargin = 15;
        qrView.setLayoutParams(qLp);
        whiteCard.addView(qrView);
        QrGenerator.displayLiveQr(qrView, myIp);

        tvHotspotInfo = new TextView(this);
        tvHotspotInfo.setText(WifiDirectEngine.runHardwareSpeedBoosterTest(this));
        tvHotspotInfo.setTextColor(Color.parseColor("#0284C7"));
        tvHotspotInfo.setTypeface(null, Typeface.BOLD);
        tvHotspotInfo.setGravity(Gravity.CENTER);
        tvHotspotInfo.setPadding(10, 10, 10, 15);
        whiteCard.addView(tvHotspotInfo);

        Switch fastSwitch = new Switch(this);
        fastSwitch.setText("⚡ 5GHz / 2.4GHz Auto-Fallback Boost   ");
        fastSwitch.setChecked(true);
        fastSwitch.setTextColor(Color.parseColor("#0F172A"));
        fastSwitch.setTypeface(null, Typeface.BOLD);
        fastSwitch.setOnCheckedChangeListener((b, checked) -> QrGenerator.displayLiveQr(qrView, myIp));
        whiteCard.addView(fastSwitch);
        root.addView(whiteCard);

        root.addView(build3DLiquidTransferCard());

        engine.broadcastReceiverRadar(myIp);
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare");
        engine.startReceiver(this, dir, callback("Receiving"));

        scroll.addView(root);
        return scroll;
    }

    // 2. SENDER UI WITH LASER SCANNER, RADAR & 3D LIQUID WAVE SPHERE
    private View buildSenderUi() {
        String myIp = MultiTransferEngine.getLocalIp(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 35, 35, 45);

        TextView tvBack = UiHelper.makeTitle(this, "←  Connect & Turbo Send", ThemeEngine.textMain(this), 20f);
        tvBack.setOnClickListener(v -> finish());
        root.addView(tvBack);

        root.addView(PermissionBox.build(this));

        // Animated Green Laser QR Frame
        LinearLayout laserBox = new LinearLayout(this);
        laserBox.setOrientation(LinearLayout.VERTICAL);
        laserBox.setGravity(Gravity.CENTER);
        laserBox.setPadding(0, 25, 0, 15);

        UiHelper.QrLaserFrameView laserFrame = new UiHelper.QrLaserFrameView(this);
        laserFrame.setLayoutParams(new LinearLayout.LayoutParams(380, 380));
        laserFrame.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
        laserBox.addView(laserFrame);
        root.addView(laserBox);

        Button btnOpenCam = UiHelper.makeBtn(this, "📷 SCAN RECEIVER QR CODE", "#16CC3E", "#FFFFFF");
        btnOpenCam.setOnClickListener(v -> scanner.launch(new ScanOptions().setOrientationLocked(true)));
        root.addView(btnOpenCam);

        tvRadarPopup = UiHelper.makeTitle(this, "📡 Auto-Radar Scanning...\nTV/PC Link: http://" + myIp + ":8080", ThemeEngine.accent(this), 14f);
        tvRadarPopup.setGravity(Gravity.CENTER);
        tvRadarPopup.setPadding(15, 20, 15, 20);
        root.addView(tvRadarPopup);

        etIp = new EditText(this);
        etIp.setHint("Or Enter Receiver IP (e.g. 192.168.43.1)");
        etIp.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        etIp.setHintTextColor(Color.parseColor(ThemeEngine.textSub(this)));
        etIp.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 30, 2));
        etIp.setPadding(35, 25, 35, 25);
        root.addView(etIp);

        Button manualBtn = UiHelper.makeBtn(this, "🚀 START TURBO TRANSFER NOW", "#2563EB", "#FFFFFF");
        manualBtn.setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            if (!ip.isEmpty()) startSend(ip);
        });
        root.addView(manualBtn);

        root.addView(build3DLiquidTransferCard());

        startRadarListen();
        scroll.addView(root);
        return scroll;
    }

    // 3D LIQUID WAVE SPHERE + ETA TIMER + PAUSE/RESUME + OFFLINE CHAT CARD
    private View build3DLiquidTransferCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(UiHelper.glassCard("#00F5D4", 42));
        card.setPadding(35, 35, 35, 35);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 30;
        card.setLayoutParams(lp);

        tvSpeed = UiHelper.makeTitle(this, "⚡ 0.0 Mbps", "#00F5D4", 26f);
        card.addView(tvSpeed);

        // 3D Liquid Wave Sphere (Shows % and Remaining Minutes/Seconds ETA)
        liquidSphere = new UiHelper.LiquidWaveSphereView(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(340, 340);
        spLp.topMargin = 20;
        spLp.bottomMargin = 20;
        liquidSphere.setLayoutParams(spLp);
        card.addView(liquidSphere);

        tvStatus = UiHelper.makeTitle(this, "2MB Smart Chunk Engine Ready", "#FFFFFF", 13.5f);
        tvStatus.setGravity(Gravity.CENTER);
        card.addView(tvStatus);

        // Pause / Resume & Offline Chat Row
        LinearLayout ctrlRow = new LinearLayout(this);
        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(-1, -2);
        crLp.topMargin = 22;
        ctrlRow.setLayoutParams(crLp);

        btnPauseResume = new Button(this);
        btnPauseResume.setText("⏸️ PAUSE");
        btnPauseResume.setAllCaps(false);
        btnPauseResume.setTextColor(Color.parseColor("#070B14"));
        btnPauseResume.setTypeface(null, Typeface.BOLD);
        btnPauseResume.setBackground(UiHelper.box("#FACC15", 40));
        btnPauseResume.setLayoutParams(new LinearLayout.LayoutParams(0, 115, 1f));
        btnPauseResume.setOnClickListener(v -> {
            engine.togglePause();
            btnPauseResume.setText(engine.isPaused() ? "▶️ RESUME" : "⏸️ PAUSE");
            tvStatus.setText(engine.isPaused() ? "⏸️ Transfer Paused (Tap Resume)" : "⚡ Resuming Transfer...");
        });

        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(20, 10));

        Button btnChat = new Button(this);
        btnChat.setText("💬 Offline Chat");
        btnChat.setAllCaps(false);
        btnChat.setTextColor(Color.WHITE);
        btnChat.setTypeface(null, Typeface.BOLD);
        btnChat.setBackground(UiHelper.box("#2563EB", 40));
        btnChat.setLayoutParams(new LinearLayout.LayoutParams(0, 115, 1f));
        btnChat.setOnClickListener(v -> showOfflineChatDialog());

        ctrlRow.addView(btnPauseResume);
        ctrlRow.addView(gap);
        ctrlRow.addView(btnChat);
        card.addView(ctrlRow);

        tvChatLog = new TextView(this);
        tvChatLog.setText("💬 Offline Chat Ready (No Internet Needed)");
        tvChatLog.setTextColor(Color.parseColor("#94A3B8"));
        tvChatLog.setPadding(0, 15, 0, 0);
        card.addView(tvChatLog);

        return card;
    }

    private void showOfflineChatDialog() {
        final EditText et = new EditText(this);
        et.setHint("Type message or paste link...");
        new AlertDialog.Builder(this)
                .setTitle("💬 Send Offline Message")
                .setView(et)
                .setPositiveButton("Send", (d, w) -> {
                    String msg = et.getText().toString().trim();
                    if (!msg.isEmpty()) {
                        engine.sendOfflineChatOrClipboard(connectedPeerIp, msg);
                        tvChatLog.setText("💬 You: " + msg);
                    }
                })
                .show();
    }

    private void showTransferModeDialog(String myIp) {
        String[] modes = {
                "⚡ Wi-Fi Direct (5GHz / 2.4GHz Auto-Fallback)",
                "((📡)) Hotspot High-Speed Mode"
        };
        new AlertDialog.Builder(this)
                .setTitle("Select Transfer Mode")
                .setItems(modes, (d, which) -> {
                    currentModeName = (which == 0) ? "Wi-Fi Direct" : "Hotspot";
                    btnModeSwitch.setText("⇄ " + currentModeName);
                    QrGenerator.displayLiveQr(qrView, myIp);
                })
                .show();
    }

    private void startRadarListen() {
        engine.scanRadar(new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String name, String ip) {
                runOnUiThread(() -> {
                    connectedPeerIp = ip;
                    tvRadarPopup.setText("🟢 FOUND: " + name + " (" + ip + ")\n👉 TAP HERE FOR ZERO-CLICK SEND!");
                    tvRadarPopup.setTextColor(Color.parseColor("#00E676"));
                    tvRadarPopup.setOnClickListener(v -> startSend(ip));
                });
            }
            @Override public void onProgress(String f, int i, int t, int p, double s) {}
            @Override public void onDone(int t, long b) {}
            @Override public void onError(String m) {}
        });
    }

    private void startSend(String ip) {
        connectedPeerIp = ip;
        tvStatus.setText("⚡ Preparing 2MB Smart Chunks for " + ip + "...");
        List<AppSession.ShareItem> toSend = new ArrayList<>(AppSession.selectedQueue);

        // If Auto-Zip is ON and user selected more than 3 files, pack into 1 high-speed bundle
        if (AppSession.autoZipEnabled && toSend.size() > 3) {
            File zip = MultiTransferEngine.createAutoZipBundle(this, toSend);
            if (zip != null && zip.exists()) {
                toSend.clear();
                toSend.add(new AppSession.ShareItem(zip.getName(), zip.getAbsolutePath(), zip.length(), "ZIP"));
            }
        }
        engine.sendFiles(this, ip, toSend, callback("Sending"));
    }

    private MultiTransferEngine.Callback callback(String tag) {
        final long totalBatchBytes = Math.max(1, AppSession.getSelectedTotalBytes());
        return new MultiTransferEngine.Callback() {
            @Override public void onRadarFound(String n, String ip) {}
            @Override public void onProgress(String f, int i, int t, int p, double s) {
                runOnUiThread(() -> {
                    long remBytes = (totalBatchBytes * (100 - p)) / 100;
                    liquidSphere.updateTransferState(p, s, remBytes);
                    tvSpeed.setText(String.format(Locale.US, "⚡ %.1f Mbps", s));
                    tvStatus.setText(tag + " (" + i + "/" + t + "): " + f);
                });
            }
            @Override public void onDone(int t, long b) {
                runOnUiThread(() -> {
                    liquidSphere.updateTransferState(100, 0, 0);
                    tvStatus.setText("✅ Completed: " + t + " files (" + AppSession.formatSize(b) + ")");
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
