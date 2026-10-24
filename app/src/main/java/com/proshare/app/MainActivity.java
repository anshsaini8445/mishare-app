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
    private WifiDirectEngine wifiDirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.preloadAllMediaAsync(this);
        webEngine = new MultiTransferEngine();
        wifiDirect = new WifiDirectEngine();

        screenContainer = new FrameLayout(this);
        screenContainer.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        setContentView(ThemeEngine.applyOneHandWrapper(this, screenContainer));
        showHomeScreen();
    }

    private void showScreen(View v) {
        screenContainer.removeAllViews();
        screenContainer.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        screenContainer.addView(v);
    }

    // 1. ULTRA-PREMIUM GLASSMORPHISM & THEME-AWARE HOME SCREEN
    private void showHomeScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 45);

        // Top Bar: ☰ Settings | Brand Name | 🎨 Theme | 🌐 Lang | ⊕ Invite
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnDrawer = UiHelper.makeTitle(this, "☰", ThemeEngine.accent(this), 26f);
        btnDrawer.setPadding(10, 10, 25, 10);
        UiHelper.attachSmoothBounce(btnDrawer);
        btnDrawer.setOnClickListener(v -> showSettingsScreen());

        TextView tvBrand = UiHelper.makeTitle(this, "HyperShare X", ThemeEngine.textMain(this), 21f);
        tvBrand.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button btnThemeQuick = new Button(this);
        btnThemeQuick.setText("🎨");
        btnThemeQuick.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 40, 2));
        btnThemeQuick.setLayoutParams(new LinearLayout.LayoutParams(110, 95));
        UiHelper.attachSmoothBounce(btnThemeQuick);
        btnThemeQuick.setOnClickListener(v -> {
            ThemeEngine.nextTheme(this);
            recreate();
        });

        View spTop = new View(this);
        spTop.setLayoutParams(new LinearLayout.LayoutParams(14, 10));

        Button btnInvite = new Button(this);
        btnInvite.setText("⊕ Invite");
        btnInvite.setAllCaps(false);
        btnInvite.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        btnInvite.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 45, 2));
        btnInvite.setLayoutParams(new LinearLayout.LayoutParams(210, 95));
        UiHelper.attachSmoothBounce(btnInvite);
        btnInvite.setOnClickListener(v -> showInviteScreen());

        topBar.addView(btnDrawer);
        topBar.addView(tvBrand);
        topBar.addView(btnThemeQuick);
        topBar.addView(spTop);
        topBar.addView(btnInvite);
        root.addView(topBar);

        // GLOWING ONE-HAND MODE + LANGUAGE BAR
        LinearLayout quickStrip = new LinearLayout(this);
        quickStrip.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams qsLp = new LinearLayout.LayoutParams(-1, -2);
        qsLp.topMargin = 28;
        quickStrip.setLayoutParams(qsLp);

        Button btnOneHand = new Button(this);
        btnOneHand.setText(LangEngine.t(this, "ONE_HAND") + (ThemeEngine.isOneHandMode(this) ? " [ON]" : ""));
        btnOneHand.setAllCaps(false);
        btnOneHand.setTextSize(12f);
        btnOneHand.setTypeface(null, Typeface.BOLD);
        btnOneHand.setTextColor(Color.parseColor("#070B14"));
        btnOneHand.setBackground(UiHelper.gradientBento("#FACC15", "#00F5D4", 40));
        btnOneHand.setLayoutParams(new LinearLayout.LayoutParams(0, 105, 1.2f));
        UiHelper.attachSmoothBounce(btnOneHand);
        btnOneHand.setOnClickListener(v -> {
            ThemeEngine.toggleOneHandMode(this);
            recreate();
        });

        View qsGap = new View(this);
        qsGap.setLayoutParams(new LinearLayout.LayoutParams(18, 10));

        Button btnLang = new Button(this);
        btnLang.setText(LangEngine.getCurrentLangName(this));
        btnLang.setAllCaps(false);
        btnLang.setTextSize(12f);
        btnLang.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        btnLang.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 40, 2));
        btnLang.setLayoutParams(new LinearLayout.LayoutParams(0, 105, 1f));
        UiHelper.attachSmoothBounce(btnLang);
        btnLang.setOnClickListener(v -> showLanguagePickerDialog());

        quickStrip.addView(btnOneHand);
        quickStrip.addView(qsGap);
        quickStrip.addView(btnLang);
        root.addView(quickStrip);

        // BIG HERO SEND & RECEIVE CIRCLES
        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.topMargin = 65;
        heroRow.setLayoutParams(hLp);

        LinearLayout sendCol = createHeroButton(LangEngine.t(this, "SEND"), true);
        sendCol.setOnClickListener(v -> startActivity(new Intent(this, FilePickerActivity.class)));

        View space = new View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(95, 10));

        LinearLayout recCol = createHeroButton(LangEngine.t(this, "RECEIVE"), false);
        recCol.setOnClickListener(v -> {
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "RECEIVE");
            startActivity(i);
        });

        heroRow.addView(sendCol);
        heroRow.addView(space);
        heroRow.addView(recCol);
        root.addView(heroRow);

        // SUPER TOOLS & PHONE CLONE GLOWING CARD
        Button btnSuperTools = new Button(this);
        btnSuperTools.setText(LangEngine.t(this, "SUPER_TOOLS") + "  (PIN • Cleaner • Zip)");
        btnSuperTools.setAllCaps(false);
        btnSuperTools.setTextColor(Color.WHITE);
        btnSuperTools.setTypeface(null, Typeface.BOLD);
        btnSuperTools.setBackground(UiHelper.gradientBento("#7C3AED", "#2563EB", 45));
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, 138);
        stLp.topMargin = 55;
        btnSuperTools.setLayoutParams(stLp);
        UiHelper.attachSmoothBounce(btnSuperTools);
        btnSuperTools.setOnClickListener(v -> SuperToolsDialog.showHub(this));
        root.addView(btnSuperTools);

        // PILL ROW: [ 🕒 History Vault ]  [ 💻 Connect to PC ]
        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-1, -2);
        pLp.topMargin = 30;
        pillRow.setLayoutParams(pLp);

        Button btnHistory = new Button(this);
        btnHistory.setText(LangEngine.t(this, "HISTORY"));
        btnHistory.setAllCaps(false);
        btnHistory.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        btnHistory.setTypeface(null, Typeface.BOLD);
        btnHistory.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 45, 2));
        btnHistory.setLayoutParams(new LinearLayout.LayoutParams(0, 135, 1f));
        UiHelper.attachSmoothBounce(btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra("START_TAB", "HISTORY");
            startActivity(i);
        });

        View pSpace = new View(this);
        pSpace.setLayoutParams(new LinearLayout.LayoutParams(25, 10));

        Button btnPc = new Button(this);
        btnPc.setText("💻 Connect to PC");
        btnPc.setAllCaps(false);
        btnPc.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        btnPc.setTypeface(null, Typeface.BOLD);
        btnPc.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), "#38BDF8", 45, 2));
        btnPc.setLayoutParams(new LinearLayout.LayoutParams(0, 135, 1f));
        UiHelper.attachSmoothBounce(btnPc);
        btnPc.setOnClickListener(v -> showWebShareModeScreen());

        pillRow.addView(btnHistory);
        pillRow.addView(pSpace);
        pillRow.addView(btnPc);
        root.addView(pillRow);

        // Status & Theme Info Card
        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 35, 2));
        infoCard.setPadding(35, 28, 35, 28);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(-1, -2);
        icLp.topMargin = 35;
        infoCard.setLayoutParams(icLp);

        infoCard.addView(UiHelper.makeTitle(this, "⚡ " + wifiDirect.getBandStatus(this), ThemeEngine.accent(this), 13.5f));
        TextView st = new TextView(this);
        st.setText("Theme: " + ThemeEngine.getThemeName(this) + "\n" + AppSession.getStorageText());
        st.setTextColor(Color.parseColor(ThemeEngine.textSub(this)));
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
        circle.setLayoutParams(new LinearLayout.LayoutParams(245, 245));

        TextView tv = UiHelper.makeTitle(this, label, ThemeEngine.textMain(this), 18f);
        tv.setPadding(0, 18, 0, 0);

        col.addView(circle);
        col.addView(tv);
        return col;
    }

    private void showLanguagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🌐 Select App Language (11 Languages)")
                .setItems(LangEngine.NAMES, (d, which) -> {
                    LangEngine.setLangIndex(this, which);
                    recreate();
                })
                .show();
    }

    private void showThemePickerDialog() {
        String[] themes = {
                "⚡ Cyber-Neon Green/Cyan",
                "🌑 AMOLED Pure Pitch Black",
                "🌊 Ocean Blue & Pure White",
                "🌅 Sunset Orange/Purple Gradient",
                "💎 Dynamic Material Glass",
                "🌗 Auto Day/Night Switch"
        };
        new AlertDialog.Builder(this)
                .setTitle("🎨 Select Ultra Theme")
                .setItems(themes, (d, which) -> {
                    ThemeEngine.setTheme(this, which);
                    recreate();
                })
                .show();
    }

    // 2. SETTINGS SCREEN (With Clearly Shining ONE-HAND MODE Button, Themes & 11 Languages)
    private void showSettingsScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("⚙️ Settings & Customization"));

        // CLEARLY SHINING ONE-HAND MODE BUTTON IN SETTINGS
        LinearLayout oneHandCard = new LinearLayout(this);
        oneHandCard.setOrientation(LinearLayout.VERTICAL);
        oneHandCard.setBackground(UiHelper.gradientBento("#FACC15", "#00F5D4", 38));
        oneHandCard.setPadding(35, 32, 35, 32);
        LinearLayout.LayoutParams ohLp = new LinearLayout.LayoutParams(-1, -2);
        ohLp.topMargin = 35;
        oneHandCard.setLayoutParams(ohLp);
        UiHelper.attachSmoothBounce(oneHandCard);

        oneHandCard.addView(UiHelper.makeTitle(this, "🖐️ ONE-HAND MODE (एक हाथ मोड) - " + (ThemeEngine.isOneHandMode(this) ? "ON" : "OFF"), "#070B14", 17f));
        TextView ohSub = new TextView(this);
        ohSub.setText("Tap here to shrink full app to bottom thumb-zone for easy single-hand use!");
        ohSub.setTextColor(Color.parseColor("#0F172A"));
        ohSub.setTypeface(null, Typeface.BOLD);
        oneHandCard.addView(ohSub);
        oneHandCard.setOnClickListener(v -> {
            ThemeEngine.toggleOneHandMode(this);
            recreate();
        });
        root.addView(oneHandCard);

        // Stats Row: Sent | Received | Files
        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 35, 2));
        statRow.setPadding(25, 35, 25, 35);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(-1, -2);
        srLp.topMargin = 30;
        statRow.setLayoutParams(srLp);

        statRow.addView(makeStatCol(AppSession.getSentText(this), "Sent"));
        statRow.addView(makeStatCol(AppSession.getRecText(this), "Received"));
        statRow.addView(makeStatCol(String.valueOf(AppSession.getFilesCount(this)), "Files"));
        root.addView(statRow);

        Button btnTheme = UiHelper.makeBtn(this, "🎨 Theme: " + ThemeEngine.getThemeName(this), ThemeEngine.card(this), ThemeEngine.textMain(this));
        btnTheme.setOnClickListener(v -> showThemePickerDialog());
        root.addView(btnTheme);

        Button btnLang = UiHelper.makeBtn(this, "🌐 Language: " + LangEngine.getCurrentLangName(this), ThemeEngine.card(this), ThemeEngine.textMain(this));
        btnLang.setOnClickListener(v -> showLanguagePickerDialog());
        root.addView(btnLang);

        Button btnTools = UiHelper.makeBtn(this, "🛠️ Open Super Tools (Phone Clone / PIN / Cleaner)", "#2563EB", "#FFFFFF");
        btnTools.setOnClickListener(v -> SuperToolsDialog.showHub(this));
        root.addView(btnTools);

        Button btnBattery = UiHelper.makeBtn(this, "🛡️ Avoid Abnormal Stop (SET Battery Stability)", "#16CC3E", "#FFFFFF");
        btnBattery.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {}
        });
        root.addView(btnBattery);

        scroll.addView(root);
        showScreen(scroll);
    }

    // 3. WEB SHARE SCREEN (Hotspot Faster Mode & Wi-Fi Convenient Mode)
    private void showWebShareModeScreen() {
        webEngine.startWebServer();
        String ip = MultiTransferEngine.getLocalIp(this);
        String url = "http://" + ip + ":" + MultiTransferEngine.WEB_PORT;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("💻 Web Share (PC / Smart TV / iOS)"));

        TextView urlBox = UiHelper.makeTitle(this, url, ThemeEngine.accent(this), 20f);
        urlBox.setGravity(Gravity.CENTER);
        urlBox.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 30, 2));
        urlBox.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(-1, -2);
        uLp.topMargin = 35;
        urlBox.setLayoutParams(uLp);
        root.addView(urlBox);

        ImageView qr = new ImageView(this);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(440, 440);
        qLp.topMargin = 35;
        qr.setLayoutParams(qLp);
        root.addView(qr);
        QrGenerator.displayLiveQr(qr, url);

        scroll.addView(root);
        showScreen(scroll);
    }

    // 4. INVITE SCREEN
    private void showInviteScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(45, 45, 45, 45);

        root.addView(createBackHeader("⊕ Invite Friends"));

        Button btnBt = UiHelper.makeBtn(this, "🦷 Bluetooth Zero-Data Invite", "#1890FF", "#FFFFFF");
        btnBt.setOnClickListener(v -> startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)));
        root.addView(btnBt);

        Button btnHot = UiHelper.makeBtn(this, "((📡)) Hotspot Web Invite", "#16CC3E", "#FFFFFF");
        btnHot.setOnClickListener(v -> showWebShareModeScreen());
        root.addView(btnHot);

        Button btnWa = UiHelper.makeBtn(this, "💬 Share via WhatsApp / Link", "#2563EB", "#FFFFFF");
        btnWa.setOnClickListener(v -> {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT, "Download HyperShare X - 240 Mbps Ultra-Fast File & Phone Clone App!");
            startActivity(Intent.createChooser(s, "Share via"));
        });
        root.addView(btnWa);

        scroll.addView(root);
        showScreen(scroll);
    }

    private LinearLayout makeStatCol(String val, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(UiHelper.makeTitle(this, val, ThemeEngine.textMain(this), 19f));
        TextView sub = new TextView(this);
        sub.setText(label);
        sub.setTextColor(Color.parseColor(ThemeEngine.textSub(this)));
        col.addView(sub);
        return col;
    }

    private LinearLayout createBackHeader(String title) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        TextView tv = UiHelper.makeTitle(this, "←   " + title, ThemeEngine.textMain(this), 20f);
        UiHelper.attachSmoothBounce(tv);
        tv.setOnClickListener(v -> showHomeScreen());
        bar.addView(tv);
        return bar;
    }

    @Override
    public void onBackPressed() {
        showHomeScreen();
    }
}
