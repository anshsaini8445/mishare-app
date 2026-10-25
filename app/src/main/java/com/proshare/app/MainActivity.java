package com.proshare.app;

import android.app.AlertDialog;
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
    private boolean onSubScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Pre-loads Apps, Videos, Photos, Songs & Files in background for 0-second opening
        AppSession.preloadAllMediaAsync(this);
        webEngine = new MultiTransferEngine();

        screenContainer = new FrameLayout(this);
        screenContainer.setBackgroundColor(Color.WHITE);
        setContentView(screenContainer);
        showHomeScreen();
    }

    private void showScreen(View v, boolean isSub) {
        onSubScreen = isSub;
        screenContainer.removeAllViews();
        screenContainer.addView(v);
    }

    // 1. CLEAN INSHARE-STYLE DAYLIGHT HOME SCREEN
    private void showHomeScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        // Top Bar: ☰ Menu | Brand Title | 🌐 Language | ⊕ Invite
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnMenu = UiHelper.makeTitle(this, "☰", "#0F172A", 26f);
        btnMenu.setPadding(10, 10, 30, 10);
        UiHelper.attachSmoothBounce(btnMenu);
        btnMenu.setOnClickListener(v -> showSettingsScreen());

        TextView tvBrand = UiHelper.makeTitle(this, "HyperShare", "#0F172A", 23f);
        tvBrand.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnLang = new Button(this);
        btnLang.setText("🌐");
        btnLang.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 45, 2));
        btnLang.setLayoutParams(new LinearLayout.LayoutParams(105, 95));
        UiHelper.attachSmoothBounce(btnLang);
        btnLang.setOnClickListener(v -> showLanguageDialog());

        View gapTop = new View(this);
        gapTop.setLayoutParams(new LinearLayout.LayoutParams(16, 10));

        Button btnInvite = new Button(this);
        btnInvite.setText("⊕ Invite");
        btnInvite.setAllCaps(false);
        btnInvite.setTextColor(Color.parseColor("#0F172A"));
        btnInvite.setTypeface(null, Typeface.BOLD);
        btnInvite.setBackground(UiHelper.bentoBox("#F8FAFC", "#CBD5E1", 50, 2));
        btnInvite.setLayoutParams(new LinearLayout.LayoutParams(230, 95));
        UiHelper.attachSmoothBounce(btnInvite);
        btnInvite.setOnClickListener(v -> showInviteScreen());

        topBar.addView(btnMenu);
        topBar.addView(tvBrand);
        topBar.addView(btnLang);
        topBar.addView(gapTop);
        topBar.addView(btnInvite);
        root.addView(topBar);

        // Big Circular SEND & RECEIVE Hero Buttons
        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.topMargin = 105;
        heroRow.setLayoutParams(hLp);

        LinearLayout sendCol = createHeroButton(LangEngine.t(this, "SEND"), true);
        sendCol.setOnClickListener(v -> {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra("START_TAB", "VIDEOS");
            startActivity(i);
        });

        View heroSpace = new View(this);
        heroSpace.setLayoutParams(new LinearLayout.LayoutParams(105, 10));

        LinearLayout recCol = createHeroButton(LangEngine.t(this, "RECEIVE"), false);
        recCol.setOnClickListener(v -> {
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "RECEIVE");
            startActivity(i);
        });

        heroRow.addView(sendCol);
        heroRow.addView(heroSpace);
        heroRow.addView(recCol);
        root.addView(heroRow);

        // Pastel Pill Row: [ 🕒 History ]  [ 💻 Connect to PC ]
        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-1, -2);
        pLp.topMargin = 90;
        pillRow.setLayoutParams(pLp);

        Button btnHistory = new Button(this);
        btnHistory.setText("🕒  History");
        btnHistory.setAllCaps(false);
        btnHistory.setTextColor(Color.parseColor("#1E293B"));
        btnHistory.setTypeface(null, Typeface.BOLD);
        btnHistory.setBackground(UiHelper.box("#EEF2FF", 50));
        btnHistory.setLayoutParams(new LinearLayout.LayoutParams(0, 140, 1f));
        UiHelper.attachSmoothBounce(btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra("START_TAB", "HISTORY");
            startActivity(i);
        });

        View pSpace = new View(this);
        pSpace.setLayoutParams(new LinearLayout.LayoutParams(32, 10));

        Button btnPc = new Button(this);
        btnPc.setText("💻  Connect to PC");
        btnPc.setAllCaps(false);
        btnPc.setTextColor(Color.parseColor("#0F172A"));
        btnPc.setTypeface(null, Typeface.BOLD);
        btnPc.setBackground(UiHelper.box("#E6F9F6", 50));
        btnPc.setLayoutParams(new LinearLayout.LayoutParams(0, 140, 1f));
        UiHelper.attachSmoothBounce(btnPc);
        btnPc.setOnClickListener(v -> showWebShareModeScreen());

        pillRow.addView(btnHistory);
        pillRow.addView(pSpace);
        pillRow.addView(btnPc);
        root.addView(pillRow);

        // Quick Category Bar: Apps | Videos | Photos | Songs | Files
        TextView tvQuick = UiHelper.makeTitle(this, "Quick Share Categories", "#475569", 14.5f);
        tvQuick.setPadding(8, 70, 8, 22);
        root.addView(tvQuick);

        LinearLayout catRow = new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        catRow.addView(makeQuickCatCard("📦", "Apps", "#ECFDF5", "APPS"));
        catRow.addView(makeQuickCatCard("🎬", "Videos", "#F5F3FF", "VIDEOS"));
        catRow.addView(makeQuickCatCard("🖼️", "Photos", "#EFF6FF", "PHOTOS"));
        catRow.addView(makeQuickCatCard("🎵", "Songs", "#FFFBEB", "SONGS"));
        root.addView(catRow);

        // Connect to Jio/iOS/PC/Tablet > Link
        TextView tvCross = new TextView(this);
        tvCross.setText("Connect to Jio / iOS / PC / Smart TV  >");
        tvCross.setTextColor(Color.parseColor("#334155"));
        tvCross.setTypeface(null, Typeface.BOLD);
        tvCross.setTextSize(14f);
        tvCross.setGravity(Gravity.CENTER);
        tvCross.setPadding(20, 65, 20, 20);
        UiHelper.attachSmoothBounce(tvCross);
        tvCross.setOnClickListener(v -> showWebShareModeScreen());
        root.addView(tvCross);

        // Clean Band & Storage Footer
        LinearLayout footerCard = new LinearLayout(this);
        footerCard.setOrientation(LinearLayout.VERTICAL);
        footerCard.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 32, 2));
        footerCard.setPadding(35, 28, 35, 28);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(-1, -2);
        fLp.topMargin = 35;
        footerCard.setLayoutParams(fLp);

        footerCard.addView(UiHelper.makeTitle(this, "⚡ Auto Band: " + WifiDirectEngine.getBestSupportedBandName(this), "#16CC3E", 13.5f));
        TextView tvStore = new TextView(this);
        tvStore.setText(AppSession.getStorageText() + "  •  " + Build.MODEL);
        tvStore.setTextColor(Color.parseColor("#64748B"));
        tvStore.setTextSize(12.5f);
        tvStore.setPadding(0, 6, 0, 0);
        footerCard.addView(tvStore);
        root.addView(footerCard);

        scroll.addView(root);
        showScreen(scroll, false);
    }

    private LinearLayout createHeroButton(String label, boolean isSend) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        UiHelper.attachSmoothBounce(col);

        UiHelper.HeroCircleView circle = new UiHelper.HeroCircleView(this, isSend);
        circle.setLayoutParams(new LinearLayout.LayoutParams(255, 255));

        TextView tv = UiHelper.makeTitle(this, label, "#0F172A", 18.5f);
        tv.setPadding(0, 18, 0, 0);

        col.addView(circle);
        col.addView(tv);
        return col;
    }

    private View makeQuickCatCard(String emoji, String title, String bgHex, final String tab) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(UiHelper.box(bgHex, 32));
        card.setPadding(16, 28, 16, 28);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(8, 0, 8, 0);
        card.setLayoutParams(lp);
        UiHelper.attachSmoothBounce(card);

        TextView e = new TextView(this);
        e.setText(emoji);
        e.setTextSize(22f);
        e.setGravity(Gravity.CENTER);

        TextView t = UiHelper.makeTitle(this, title, "#1E293B", 12.5f);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 8, 0, 0);

        card.addView(e);
        card.addView(t);
        card.setOnClickListener(v -> {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra("START_TAB", tab);
            startActivity(i);
        });
        return card;
    }

    // 2. WEB SHARE SCREEN (Hotspot Mode - Faster & Wi-Fi Mode - Convenient)
    private void showWebShareModeScreen() {
        webEngine.startWebServer();
        final String ip = MultiTransferEngine.getLocalIp(this);
        final String url = "http://" + ip + ":" + MultiTransferEngine.WEB_PORT;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("Web Share"));

        TextView sub = new TextView(this);
        sub.setText("Select a transmission mode");
        sub.setTextColor(Color.parseColor("#64748B"));
        sub.setPadding(5, 25, 5, 30);
        root.addView(sub);

        // Card 1: Hotspot Mode (Faster)
        LinearLayout hotCard = new LinearLayout(this);
        hotCard.setOrientation(LinearLayout.VERTICAL);
        hotCard.setGravity(Gravity.CENTER);
        hotCard.setBackground(UiHelper.bentoBox("#FFFFFF", "#E2E8F0", 38, 2));
        hotCard.setPadding(40, 45, 40, 45);
        UiHelper.attachSmoothBounce(hotCard);

        TextView badge1 = new TextView(this);
        badge1.setText("  Faster  ");
        badge1.setTextColor(Color.parseColor("#B45309"));
        badge1.setBackground(UiHelper.box("#FEF3C7", 20));
        hotCard.addView(badge1);

        TextView ic1 = UiHelper.makeTitle(this, "((📡))", "#FFFFFF", 25f);
        ic1.setGravity(Gravity.CENTER);
        ic1.setBackground(UiHelper.circleGradient("#16CC3E", "#10B981"));
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(180, 180);
        icLp.topMargin = 16;
        ic1.setLayoutParams(icLp);
        hotCard.addView(ic1);

        TextView t1 = UiHelper.makeTitle(this, "Hotspot Mode", "#0F172A", 18f);
        t1.setPadding(0, 18, 0, 6);
        hotCard.addView(t1);

        TextView d1 = new TextView(this);
        d1.setText("The other device connects to this phone's hotspot. No internet data usage.");
        d1.setTextColor(Color.parseColor("#64748B"));
        d1.setGravity(Gravity.CENTER);
        hotCard.addView(d1);
        hotCard.setOnClickListener(v -> showWebShareSteps("Hotspot Mode", url));
        root.addView(hotCard);

        // Card 2: Wi-Fi Mode (Convenient)
        LinearLayout wifiCard = new LinearLayout(this);
        wifiCard.setOrientation(LinearLayout.VERTICAL);
        wifiCard.setGravity(Gravity.CENTER);
        wifiCard.setBackground(UiHelper.bentoBox("#FFFFFF", "#E2E8F0", 38, 2));
        wifiCard.setPadding(40, 45, 40, 45);
        LinearLayout.LayoutParams wcLp = new LinearLayout.LayoutParams(-1, -2);
        wcLp.topMargin = 32;
        wifiCard.setLayoutParams(wcLp);
        UiHelper.attachSmoothBounce(wifiCard);

        TextView badge2 = new TextView(this);
        badge2.setText("  Convenient  ");
        badge2.setTextColor(Color.parseColor("#B45309"));
        badge2.setBackground(UiHelper.box("#FEF3C7", 20));
        wifiCard.addView(badge2);

        TextView ic2 = UiHelper.makeTitle(this, "📶", "#FFFFFF", 25f);
        ic2.setGravity(Gravity.CENTER);
        ic2.setBackground(UiHelper.circleGradient("#1890FF", "#2563EB"));
        ic2.setLayoutParams(icLp);
        wifiCard.addView(ic2);

        TextView t2 = UiHelper.makeTitle(this, "Wi-Fi Mode", "#0F172A", 18f);
        t2.setPadding(0, 18, 0, 6);
        wifiCard.addView(t2);

        TextView d2 = new TextView(this);
        d2.setText("Phone and the other device connect to the same Wi-Fi network.");
        d2.setTextColor(Color.parseColor("#64748B"));
        d2.setGravity(Gravity.CENTER);
        wifiCard.addView(d2);
        wifiCard.setOnClickListener(v -> showWebShareSteps("Wi-Fi Mode", url));
        root.addView(wifiCard);

        scroll.addView(root);
        showScreen(scroll, true);
    }

    private void showWebShareSteps(String modeTitle, String url) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(45, 45, 45, 45);

        LinearLayout header = new LinearLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView back = UiHelper.makeTitle(this, "←  " + modeTitle, "#0F172A", 20f);
        back.setOnClickListener(v -> showWebShareModeScreen());
        header.addView(back);
        root.addView(header);

        TextView s1 = UiHelper.makeTitle(this, "Step 1", "#0F172A", 18f);
        s1.setPadding(0, 35, 0, 12);
        root.addView(s1);

        TextView info1 = new TextView(this);
        info1.setText("Connect your PC, Laptop, iPhone or Smart TV to the same Hotspot / Wi-Fi.");
        info1.setTextColor(Color.parseColor("#475569"));
        info1.setBackground(UiHelper.box("#F1F5F9", 24));
        info1.setPadding(35, 28, 35, 28);
        root.addView(info1);

        TextView s2 = UiHelper.makeTitle(this, "Step 2", "#0F172A", 18f);
        s2.setPadding(0, 35, 0, 12);
        root.addView(s2);

        TextView ask = new TextView(this);
        ask.setText("Enter the following URL into the browser:");
        ask.setTextColor(Color.parseColor("#64748B"));
        root.addView(ask);

        TextView urlBox = UiHelper.makeTitle(this, url, "#1890FF", 21f);
        urlBox.setGravity(Gravity.CENTER);
        urlBox.setBackground(UiHelper.bentoBox("#F0F9FF", "#BAE6FD", 24, 2));
        urlBox.setPadding(30, 28, 30, 28);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(-1, -2);
        uLp.topMargin = 18;
        urlBox.setLayoutParams(uLp);
        root.addView(urlBox);

        ImageView qr = new ImageView(this);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(420, 420);
        qLp.topMargin = 30;
        qr.setLayoutParams(qLp);
        root.addView(qr);
        QrGenerator.displayLiveQr(qr, url);

        scroll.addView(root);
        showScreen(scroll, true);
    }

    // 3. INVITE SCREEN (Like InShare 00:54)
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

        TextView sub1 = new TextView(this);
        sub1.setText("Share with friends nearby");
        sub1.setTextColor(Color.parseColor("#64748B"));
        root.addView(sub1);

        Button btnBt = UiHelper.makeBtn(this, "🦷  Bluetooth", "#1890FF", "#FFFFFF");
        btnBt.setOnClickListener(v -> startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)));
        root.addView(btnBt);

        Button btnHot = UiHelper.makeBtn(this, "((📡))  Hotspot", "#16CC3E", "#FFFFFF");
        btnHot.setOnClickListener(v -> showWebShareModeScreen());
        root.addView(btnHot);

        TextView t2 = UiHelper.makeTitle(this, "Internet required", "#0F172A", 17f);
        t2.setPadding(0, 50, 0, 6);
        root.addView(t2);

        Button btnLink = UiHelper.makeBtn(this, "🔗  Share Link", "#F1F5F9", "#1890FF");
        btnLink.setOnClickListener(v -> shareLinkIntent());
        root.addView(btnLink);

        Button btnWa = UiHelper.makeBtn(this, "💬  WhatsApp", "#16CC3E", "#FFFFFF");
        btnWa.setOnClickListener(v -> shareLinkIntent());
        root.addView(btnWa);

        scroll.addView(root);
        showScreen(scroll, true);
    }

    // 4. CLEAN SETTINGS & STATS SCREEN (Like InShare 01:12)
    private void showSettingsScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("Settings"));

        TextView dev = UiHelper.makeTitle(this, "👤  " + Build.MODEL, "#0F172A", 20f);
        dev.setGravity(Gravity.CENTER);
        dev.setPadding(0, 35, 0, 30);
        root.addView(dev);

        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setBackground(UiHelper.bentoBox("#F8FAFC", "#E2E8F0", 32, 2));
        statRow.setPadding(25, 35, 25, 35);

        statRow.addView(makeStatCol(AppSession.getSentText(this), "Sent"));
        statRow.addView(makeStatCol(AppSession.getRecText(this), "Received"));
        statRow.addView(makeStatCol(String.valueOf(AppSession.getFilesCount(this)), "Files"));
        root.addView(statRow);

        TextView gen = UiHelper.makeTitle(this, "GENERAL SETTINGS", "#16CC3E", 13f);
        gen.setPadding(0, 45, 0, 15);
        root.addView(gen);

        Button btnLang = UiHelper.makeBtn(this, "🌐 Language: " + LangEngine.getCurrentLangName(this), "#F8FAFC", "#0F172A");
        btnLang.setOnClickListener(v -> showLanguageDialog());
        root.addView(btnLang);

        Button btnApkBackup = UiHelper.makeBtn(this, "📦 Backup Installed Apps to Download Folder", "#F8FAFC", "#0F172A");
        btnApkBackup.setOnClickListener(v -> SuperToolsDialog.backupSelectedApks(this));
        root.addView(btnApkBackup);

        TextView loc = UiHelper.makeTitle(this, "Save Location\n/storage/emulated/0/Download/HyperShare", "#334155", 14f);
        loc.setPadding(5, 25, 5, 20);
        root.addView(loc);

        LinearLayout stopRow = new LinearLayout(this);
        stopRow.setGravity(Gravity.CENTER_VERTICAL);
        stopRow.setPadding(5, 15, 5, 25);

        TextView tvStop = new TextView(this);
        tvStop.setText("Avoid Abnormal Stop\nTurn off system battery optimization for uninterrupted high-speed transfers.");
        tvStop.setTextColor(Color.parseColor("#475569"));
        tvStop.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnSet = new Button(this);
        btnSet.setText("SET");
        btnSet.setTextColor(Color.WHITE);
        btnSet.setBackground(UiHelper.box("#16CC3E", 24));
        btnSet.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {}
        });

        stopRow.addView(tvStop);
        stopRow.addView(btnSet);
        root.addView(stopRow);

        scroll.addView(root);
        showScreen(scroll, true);
    }

    private void showLanguageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(LangEngine.NAMES, (d, which) -> {
                    LangEngine.setLangIndex(this, which);
                    showHomeScreen();
                })
                .show();
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
        bar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView tv = UiHelper.makeTitle(this, "←   " + title, "#0F172A", 20f);
        UiHelper.attachSmoothBounce(tv);
        tv.setOnClickListener(v -> showHomeScreen());
        bar.addView(tv);
        return bar;
    }

    private void shareLinkIntent() {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, "Download HyperShare - High-Speed 6GHz/5GHz/2.4GHz File Sharing App!");
        startActivity(Intent.createChooser(s, "Share via"));
    }

    @Override
    public void onBackPressed() {
        if (onSubScreen) {
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }
}
