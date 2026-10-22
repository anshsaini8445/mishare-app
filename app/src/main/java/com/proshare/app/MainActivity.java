package com.proshare.app;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FrameLayout screenContainer;
    private MultiTransferEngine webEngine;
    private WifiDirectEngine wifiDirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Zero-Lag Preload in Background immediately on launch!
        AppSession.preloadAllMediaAsync(this);
        webEngine = new MultiTransferEngine();
        wifiDirect = new WifiDirectEngine();

        screenContainer = new FrameLayout(this);
        screenContainer.setBackgroundColor(Color.WHITE);
        setContentView(screenContainer);
        showHomeScreen();
    }

    private void showScreen(View v) {
        screenContainer.removeAllViews();
        screenContainer.addView(v);
    }

    // 1. MAKKHAN-SMOOTH HOME SCREEN (Upgraded from Video 00:03)
    private void showHomeScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        // Top Bar: Menu (☰) + Brand Name + Invite Pill (⊕ Invite)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnDrawer = UiHelper.makeTitle(this, "☰", "#1E293B", 26f);
        btnDrawer.setPadding(10, 10, 35, 10);
        UiHelper.attachSmoothBounce(btnDrawer);
        btnDrawer.setOnClickListener(v -> showSettingsScreen());

        TextView tvBrand = UiHelper.makeTitle(this, "HyperShare", "#0F172A", 22f);
        tvBrand.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnInvite = new Button(this);
        btnInvite.setText("⊕ Invite");
        btnInvite.setAllCaps(false);
        btnInvite.setTextColor(Color.parseColor("#1E293B"));
        btnInvite.setBackground(UiHelper.box("#F1F5F9", 50));
        btnInvite.setLayoutParams(new LinearLayout.LayoutParams(250, 96));
        UiHelper.attachSmoothBounce(btnInvite);
        btnInvite.setOnClickListener(v -> showInviteScreen());

        topBar.addView(btnDrawer);
        topBar.addView(tvBrand);
        topBar.addView(btnInvite);
        root.addView(topBar);

        // Big Circular SEND & RECEIVE Hero Buttons (Vector Plane & Arrow - Video 00:03)
        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.topMargin = 110;
        heroRow.setLayoutParams(hLp);

        LinearLayout sendCol = createHeroButton("Send", true);
        sendCol.setOnClickListener(v -> startActivity(new Intent(this, FilePickerActivity.class)));

        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(110, 10));

        LinearLayout recCol = createHeroButton("Receive", false);
        recCol.setOnClickListener(v -> {
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "RECEIVE");
            startActivity(i);
        });

        heroRow.addView(sendCol);
        heroRow.addView(space);
        heroRow.addView(recCol);
        root.addView(heroRow);

        // Pastel Pill Buttons: [ 🕒 History ]  [ 💻 Connect to PC ] (Video 00:04)
        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-1, -2);
        pLp.topMargin = 95;
        pillRow.setLayoutParams(pLp);

        Button btnHistory = new Button(this);
        btnHistory.setText("🕒  History");
        btnHistory.setAllCaps(false);
        btnHistory.setTextColor(Color.parseColor("#1E293B"));
        btnHistory.setTypeface(null, Typeface.BOLD);
        btnHistory.setBackground(UiHelper.box("#EEF2FF", 50));
        btnHistory.setLayoutParams(new LinearLayout.LayoutParams(0, 145, 1f));
        UiHelper.attachSmoothBounce(btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra("START_TAB", "HISTORY");
            startActivity(i);
        });

        View pSpace = new View(this);
        pSpace.setLayoutParams(new LinearLayout.LayoutParams(35, 10));

        Button btnPc = new Button(this);
        btnPc.setText("💻  Connect to PC");
        btnPc.setAllCaps(false);
        btnPc.setTextColor(Color.parseColor("#0F172A"));
        btnPc.setTypeface(null, Typeface.BOLD);
        btnPc.setBackground(UiHelper.box("#E6F9F6", 50));
        btnPc.setLayoutParams(new LinearLayout.LayoutParams(0, 145, 1f));
        UiHelper.attachSmoothBounce(btnPc);
        btnPc.setOnClickListener(v -> showWebShareModeScreen());

        pillRow.addView(btnHistory);
        pillRow.addView(pSpace);
        pillRow.addView(btnPc);
        root.addView(pillRow);

        // Connect to Jio/iOS/PC/Tablet > Link (Video 00:04)
        TextView tvCross = new TextView(this);
        tvCross.setText("Connect to Jio / iOS / PC / Smart TV  >");
        tvCross.setTextColor(Color.parseColor("#334155"));
        tvCross.setTextSize(14.5f);
        tvCross.setGravity(Gravity.CENTER);
        tvCross.setPadding(20, 75, 20, 30);
        UiHelper.attachSmoothBounce(tvCross);
        tvCross.setOnClickListener(v -> showWebShareModeScreen());
        root.addView(tvCross);

        // Turbo Hardware Status Card at Bottom
        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 35, 2));
        infoCard.setPadding(35, 30, 35, 30);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(-1, -2);
        icLp.topMargin = 60;
        infoCard.setLayoutParams(icLp);

        infoCard.addView(UiHelper.makeTitle(this, "⚡ Zero-Lag Turbo Engine (" + wifiDirect.getBandStatus(this) + ")", "#0F172A", 13.5f));
        TextView st = new TextView(this);
        st.setText(AppSession.getStorageText() + "  •  Device: " + Build.MODEL);
        st.setTextColor(Color.parseColor("#64748B"));
        st.setTextSize(12.5f);
        st.setPadding(0, 8, 0, 0);
        infoCard.addView(st);
        root.addView(infoCard);

        scroll.addView(root);
        showScreen(scroll);
    }

    private LinearLayout createHeroButton(String label, boolean isSend) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        UiHelper.attachSmoothBounce(col);

        UiHelper.HeroCircleView circle = new UiHelper.HeroCircleView(this, isSend);
        circle.setLayoutParams(new LinearLayout.LayoutParams(260, 260));

        TextView tv = UiHelper.makeTitle(this, label, "#0F172A", 19f);
        tv.setPadding(0, 22, 0, 0);

        col.addView(circle);
        col.addView(tv);
        return col;
    }

    // 2. WEB SHARE SCREEN: HOTSPOT MODE (FASTER) & WI-FI MODE (CONVENIENT) (Video 00:14 - 00:30)
    private void showWebShareModeScreen() {
        webEngine.startWebServer();
        String ip = MultiTransferEngine.getLocalIp(this);
        String url = "http://" + ip + ":" + MultiTransferEngine.WEB_PORT;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("Web Share"));

        TextView sub = new TextView(this);
        sub.setText("Select a transmission mode");
        sub.setTextColor(Color.parseColor("#64748B"));
        sub.setPadding(5, 25, 5, 35);
        root.addView(sub);

        // Mode 1: Hotspot Mode (Faster) - Video 00:15
        LinearLayout hotspotCard = new LinearLayout(this);
        hotspotCard.setOrientation(LinearLayout.VERTICAL);
        hotspotCard.setGravity(Gravity.CENTER);
        hotspotCard.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 40, 2));
        hotspotCard.setPadding(40, 50, 40, 50);
        UiHelper.attachSmoothBounce(hotspotCard);

        TextView badgeFast = new TextView(this);
        badgeFast.setText("  Faster  ");
        badgeFast.setTextColor(Color.parseColor("#B45309"));
        badgeFast.setBackground(UiHelper.box("#FEF3C7", 20));
        hotspotCard.addView(badgeFast);

        TextView iconHot = UiHelper.makeTitle(this, "((📡))", "#FFFFFF", 26f);
        iconHot.setGravity(Gravity.CENTER);
        iconHot.setBackground(UiHelper.circleGradient("#16CC3E", "#10B981"));
        LinearLayout.LayoutParams ihLp = new LinearLayout.LayoutParams(190, 190);
        ihLp.topMargin = 15;
        iconHot.setLayoutParams(ihLp);
        hotspotCard.addView(iconHot);

        TextView titleHot = UiHelper.makeTitle(this, "Hotspot Mode", "#0F172A", 18f);
        titleHot.setPadding(0, 20, 0, 8);
        hotspotCard.addView(titleHot);

        TextView descHot = new TextView(this);
        descHot.setText("The other device connects to the phone's hotspot. No internet data usage.");
        descHot.setTextColor(Color.parseColor("#64748B"));
        descHot.setGravity(Gravity.CENTER);
        hotspotCard.addView(descHot);

        hotspotCard.setOnClickListener(v -> showWebShareStepDetail("Hotspot Mode", "AndroidShare_5G", "hyper1234", url));
        root.addView(hotspotCard);

        // Mode 2: Wi-Fi Mode (Convenient) - Video 00:25
        LinearLayout wifiCard = new LinearLayout(this);
        wifiCard.setOrientation(LinearLayout.VERTICAL);
        wifiCard.setGravity(Gravity.CENTER);
        wifiCard.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 40, 2));
        wifiCard.setPadding(40, 50, 40, 50);
        LinearLayout.LayoutParams wcLp = new LinearLayout.LayoutParams(-1, -2);
        wcLp.topMargin = 35;
        wifiCard.setLayoutParams(wcLp);
        UiHelper.attachSmoothBounce(wifiCard);

        TextView badgeConv = new TextView(this);
        badgeConv.setText("  Convenient  ");
        badgeConv.setTextColor(Color.parseColor("#B45309"));
        badgeConv.setBackground(UiHelper.box("#FEF3C7", 20));
        wifiCard.addView(badgeConv);

        TextView iconWifi = UiHelper.makeTitle(this, "📶", "#FFFFFF", 26f);
        iconWifi.setGravity(Gravity.CENTER);
        iconWifi.setBackground(UiHelper.circleGradient("#1890FF", "#2563EB"));
        iconWifi.setLayoutParams(ihLp);
        wifiCard.addView(iconWifi);

        TextView titleWifi = UiHelper.makeTitle(this, "Wi-Fi Mode", "#0F172A", 18f);
        titleWifi.setPadding(0, 20, 0, 8);
        wifiCard.addView(titleWifi);

        TextView descWifi = new TextView(this);
        descWifi.setText("Phone and the other device (PC / LED TV / Laptop) connect to the same Wi-Fi.");
        descWifi.setTextColor(Color.parseColor("#64748B"));
        descWifi.setGravity(Gravity.CENTER);
        wifiCard.addView(descWifi);

        wifiCard.setOnClickListener(v -> showWebShareStepDetail("Wi-Fi Mode", "Same Wi-Fi Network", "Connected", url));
        root.addView(wifiCard);

        scroll.addView(root);
        showScreen(scroll);
    }

    // WEB SHARE STEP 1 & STEP 2 (Instant Zero-Lag - Video 00:22)
    private void showWebShareStepDetail(String modeTitle, String netName, String pass, String url) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(45, 45, 45, 45);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView back = UiHelper.makeTitle(this, "←  " + modeTitle, "#0F172A", 20f);
        UiHelper.attachSmoothBounce(back);
        back.setOnClickListener(v -> showWebShareModeScreen());
        header.addView(back);
        root.addView(header);

        TextView step1 = UiHelper.makeTitle(this, "Step 1", "#0F172A", 18f);
        step1.setPadding(0, 35, 0, 10);
        root.addView(step1);

        LinearLayout box1 = new LinearLayout(this);
        box1.setOrientation(LinearLayout.VERTICAL);
        box1.setBackground(UiHelper.box("#F1F5F9", 25));
        box1.setPadding(35, 30, 35, 30);
        box1.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView tvNet = UiHelper.makeTitle(this, "Network: " + netName + "\nStatus/Pass: " + pass, "#1890FF", 15f);
        box1.addView(tvNet);
        root.addView(box1);

        TextView step2 = UiHelper.makeTitle(this, "Step 2", "#0F172A", 18f);
        step2.setPadding(0, 40, 0, 10);
        root.addView(step2);

        TextView askUrl = new TextView(this);
        askUrl.setText("Enter the following URL into the browser (PC / TV / Phone):");
        askUrl.setTextColor(Color.parseColor("#64748B"));
        root.addView(askUrl);

        TextView urlBox = UiHelper.makeTitle(this, url, "#1890FF", 21f);
        urlBox.setGravity(Gravity.CENTER);
        urlBox.setBackground(UiHelper.bentoBox("#F0F9FF", "#BAE6FD", 25, 2));
        urlBox.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(-1, -2);
        uLp.topMargin = 20;
        urlBox.setLayoutParams(uLp);
        root.addView(urlBox);

        ImageView qr = new ImageView(this);
        qr.setImageBitmap(QrGenerator.createAutoQr(url));
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(420, 420);
        qLp.topMargin = 35;
        qr.setLayoutParams(qLp);
        root.addView(qr);

        scroll.addView(root);
        showScreen(scroll);
    }

    // 3. INVITE TO INSTALL SCREEN (Video 00:54)
    private void showInviteScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("Invite to install HyperShare"));

        TextView t1 = UiHelper.makeTitle(this, "No internet needed", "#0F172A", 17f);
        t1.setPadding(0, 40, 0, 6);
        root.addView(t1);

        TextView s1 = new TextView(this);
        s1.setText("Share with friends nearby");
        s1.setTextColor(Color.parseColor("#64748B"));
        root.addView(s1);

        Button btnBt = UiHelper.makeBtn(this, "🦷  Bluetooth", "#1890FF", "#FFFFFF");
        btnBt.setOnClickListener(v -> startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)));
        root.addView(btnBt);

        Button btnHot = UiHelper.makeBtn(this, "((📡))  Hotspot", "#16CC3E", "#FFFFFF");
        btnHot.setOnClickListener(v -> showWebShareModeScreen());
        root.addView(btnHot);

        TextView t2 = UiHelper.makeTitle(this, "Internet required", "#0F172A", 17f);
        t2.setPadding(0, 55, 0, 6);
        root.addView(t2);

        Button btnLink = UiHelper.makeBtn(this, "🔗  Share Link", "#F1F5F9", "#1890FF");
        btnLink.setOnClickListener(v -> shareLinkIntent());
        root.addView(btnLink);

        Button btnWa = UiHelper.makeBtn(this, "💬  WhatsApp", "#16CC3E", "#FFFFFF");
        btnWa.setOnClickListener(v -> shareLinkIntent());
        root.addView(btnWa);

        scroll.addView(root);
        showScreen(scroll);
    }

    // 4. SETTINGS & STATS SCREEN (Video 01:12)
    private void showSettingsScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("Settings"));

        TextView dev = UiHelper.makeTitle(this, "👤  " + Build.MODEL, "#0F172A", 20f);
        dev.setGravity(Gravity.CENTER);
        dev.setPadding(0, 40, 0, 35);
        root.addView(dev);

        // Sent | Received | Files Row (Video 01:12)
        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 35, 2));
        statRow.setPadding(25, 35, 25, 35);

        statRow.addView(makeStatCol(AppSession.getSentText(this), "Sent"));
        statRow.addView(makeStatCol(AppSession.getRecText(this), "Received"));
        statRow.addView(makeStatCol(String.valueOf(AppSession.getFilesCount(this)), "Files"));
        root.addView(statRow);

        TextView gen = UiHelper.makeTitle(this, "GENERAL SETTINGS", "#10B981", 13f);
        gen.setPadding(0, 45, 0, 15);
        root.addView(gen);

        TextView loc = UiHelper.makeTitle(this, "Save Location\n/storage/emulated/0/Download/HyperShare", "#334155", 14.5f);
        loc.setPadding(0, 15, 0, 25);
        root.addView(loc);

        // Avoid Abnormal Stop + SET Button (Video 01:13)
        LinearLayout stopRow = new LinearLayout(this);
        stopRow.setGravity(Gravity.CENTER_VERTICAL);
        stopRow.setPadding(0, 15, 0, 25);

        TextView tvStop = new TextView(this);
        tvStop.setText("Avoid Abnormal Stop\nTurn off system battery optimization to strengthen stability.");
        tvStop.setTextColor(Color.parseColor("#334155"));
        tvStop.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnSet = new Button(this);
        btnSet.setText("SET");
        btnSet.setTextColor(Color.WHITE);
        btnSet.setBackground(UiHelper.box("#16CC3E", 25));
        UiHelper.attachSmoothBounce(btnSet);
        btnSet.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {}
        });

        stopRow.addView(tvStop);
        stopRow.addView(btnSet);
        root.addView(stopRow);

        scroll.addView(root);
        showScreen(scroll);
    }

    private LinearLayout makeStatCol(String val, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(UiHelper.makeTitle(this, val, "#0F172A", 20f));
        TextView sub = new TextView(this);
        sub.setText(label);
        sub.setTextColor(Color.parseColor("#64748B"));
        col.addView(sub);
        return col;
    }

    private LinearLayout createBackHeader(String title) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv = UiHelper.makeTitle(this, "←   " + title, "#0F172A", 20f);
        UiHelper.attachSmoothBounce(tv);
        tv.setOnClickListener(v -> showHomeScreen());
        bar.addView(tv);
        return bar;
    }

    private void shareLinkIntent() {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, "Download HyperShare - Instant Zero-Lag File & Video Transfer App!");
        startActivity(Intent.createChooser(s, "Share via"));
    }

    @Override
    public void onBackPressed() {
        showHomeScreen();
    }
}
