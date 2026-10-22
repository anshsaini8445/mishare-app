package com.proshare.app;

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

    private FrameLayout contentContainer;
    private Button navHome, navHistory, navWeb;
    private MultiTransferEngine webEngine;
    private WifiDirectEngine wifiCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webEngine = new MultiTransferEngine();
        wifiCheck = new WifiDirectEngine();

        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setBackgroundColor(Color.parseColor("#070B14"));

        contentContainer = new FrameLayout(this);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        master.addView(contentContainer);

        // SLEEK BOTTOM NAVIGATION BAR
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setBackground(UiHelper.bentoBox("#0F172A", "#1E293B", 0, 2));
        bottomNav.setPadding(25, 18, 25, 18);

        navHome = createNavTab("⚡ Home", true);
        navHistory = createNavTab("📊 History", false);
        navWeb = createNavTab("📺 TV / PC Hub", false);

        navHome.setOnClickListener(v -> switchTab(0));
        navHistory.setOnClickListener(v -> switchTab(1));
        navWeb.setOnClickListener(v -> switchTab(2));

        bottomNav.addView(navHome);
        bottomNav.addView(navHistory);
        bottomNav.addView(navWeb);
        master.addView(bottomNav);

        setContentView(master);
        switchTab(0);
    }

    private Button createNavTab(String title, boolean active) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(13f);
        b.setTypeface(null, Typeface.BOLD);
        UiHelper.enableTvFocus(b);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 1f));
        styleNavTab(b, active);
        return b;
    }

    private void styleNavTab(Button b, boolean active) {
        if (active) {
            b.setTextColor(Color.parseColor("#070B14"));
            b.setBackground(UiHelper.box("#00F5D4", 30));
        } else {
            b.setTextColor(Color.parseColor("#94A3B8"));
            b.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void switchTab(int index) {
        styleNavTab(navHome, index == 0);
        styleNavTab(navHistory, index == 1);
        styleNavTab(navWeb, index == 2);

        contentContainer.removeAllViews();
        if (index == 0) contentContainer.addView(buildBentoHomeTab());
        else if (index == 1) contentContainer.addView(buildBentoHistoryTab());
        else contentContainer.addView(buildBentoWebTab());
    }

    private View buildBentoHomeTab() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 45, 35, 45);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = UiHelper.makeTitle(this, "⚡ HYPERSHARE X", "#00F5D4", 24f);
        brand.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        Button invitePill = new Button(this);
        invitePill.setText("⊕ Invite");
        invitePill.setAllCaps(false);
        invitePill.setTextColor(Color.WHITE);
        invitePill.setBackground(UiHelper.bentoBox("#1E293B", "#38BDF8", 30, 2));
        UiHelper.enableTvFocus(invitePill);
        invitePill.setOnClickListener(v -> shareAppLink());

        header.addView(brand);
        header.addView(invitePill);
        root.addView(header);

        // Dual-Band 2.4GHz + 5GHz + RAM Status Bento Card
        int bufKb = MultiTransferEngine.getDynamicBufferSize(this) / 1024;
        LinearLayout turboBento = new LinearLayout(this);
        turboBento.setOrientation(LinearLayout.VERTICAL);
        turboBento.setBackground(UiHelper.bentoBox("#11192E", "#00F5D4", 38, 2));
        turboBento.setPadding(35, 30, 35, 30);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(-1, -2);
        tbLp.topMargin = 35;
        turboBento.setLayoutParams(tbLp);

        turboBento.addView(UiHelper.makeTitle(this, "🟢 " + wifiCheck.getBandStatus(this), "#00F5D4", 14f));
        TextView sub = new TextView(this);
        sub.setText("Device: " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ") • Buffer: " + bufKb + " KB");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setPadding(0, 8, 0, 0);
        turboBento.addView(sub);
        root.addView(turboBento);

        // SEND & RECEIVE Hero Bento Cards
        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams hrLp = new LinearLayout.LayoutParams(-1, -2);
        hrLp.topMargin = 35;
        heroRow.setLayoutParams(hrLp);

        LinearLayout sendBento = createHeroBento("🚀\nSEND", "Phone • TV • PC", "#00F5D4", "#10B981");
        sendBento.setOnClickListener(v -> startActivity(new Intent(this, FilePickerActivity.class)));

        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(28, 10));

        LinearLayout recBento = createHeroBento("📡\nRECEIVE", "2.4G / 5G Radar", "#38BDF8", "#2563EB");
        recBento.setOnClickListener(v -> {
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "RECEIVE");
            startActivity(i);
        });

        heroRow.addView(sendBento);
        heroRow.addView(gap);
        heroRow.addView(recBento);
        root.addView(heroRow);

        TextView catHeader = UiHelper.makeTitle(this, "One-Tap Bento Categories", "#E2E8F0", 16f);
        catHeader.setPadding(5, 40, 5, 20);
        root.addView(catHeader);

        LinearLayout gridRow1 = new LinearLayout(this);
        gridRow1.addView(createMiniBento("🎬 Videos", "Movies & Clips", "#A855F7", "VIDEOS"));
        View g1 = new View(this); g1.setLayoutParams(new LinearLayout.LayoutParams(25, 10));
        gridRow1.addView(g1);
        gridRow1.addView(createMiniBento("🖼️ Photos", "HD Gallery", "#38BDF8", "PHOTOS"));
        root.addView(gridRow1);

        LinearLayout gridRow2 = new LinearLayout(this);
        LinearLayout.LayoutParams gr2Lp = new LinearLayout.LayoutParams(-1, -2);
        gr2Lp.topMargin = 25;
        gridRow2.setLayoutParams(gr2Lp);
        gridRow2.addView(createMiniBento("📦 Apps (APK)", "Phone & TV Apps", "#10B981", "APPS"));
        View g2 = new View(this); g2.setLayoutParams(new LinearLayout.LayoutParams(25, 10));
        gridRow2.addView(g2);
        gridRow2.addView(createMiniBento("👤 Contacts", "All VCF Cards", "#F59E0B", "CONTACTS"));
        root.addView(gridRow2);

        scroll.addView(root);
        return scroll;
    }

    private View buildBentoHistoryTab() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(35, 45, 35, 45);

        root.addView(UiHelper.makeTitle(this, "📊 Transfer Statistics & Storage", "#00F5D4", 22f));

        LinearLayout statGrid = new LinearLayout(this);
        LinearLayout.LayoutParams sgLp = new LinearLayout.LayoutParams(-1, -2);
        sgLp.topMargin = 35;
        statGrid.setLayoutParams(sgLp);

        statGrid.addView(createStatBento("🚀 SENT", AppSession.getSentText(this), "#00F5D4"));
        View sGap = new View(this); sGap.setLayoutParams(new LinearLayout.LayoutParams(25, 10));
        statGrid.addView(sGap);
        statGrid.addView(createStatBento("📥 RECEIVED", AppSession.getRecText(this), "#38BDF8"));
        root.addView(statGrid);

        LinearLayout filesCard = new LinearLayout(this);
        filesCard.setOrientation(LinearLayout.VERTICAL);
        filesCard.setBackground(UiHelper.bentoBox("#11192E", "#263554", 35, 2));
        filesCard.setPadding(35, 35, 35, 35);
        LinearLayout.LayoutParams fcLp = new LinearLayout.LayoutParams(-1, -2);
        fcLp.topMargin = 25;
        filesCard.setLayoutParams(fcLp);

        filesCard.addView(UiHelper.makeTitle(this, "📦 Total Files Transferred: " + AppSession.getFilesCount(this), "#FACC15", 18f));
        TextView st = new TextView(this);
        st.setText(AppSession.getStorageText() + "\nSave Path: /Download/HyperShare");
        st.setTextColor(Color.parseColor("#CBD5E1"));
        st.setPadding(0, 15, 0, 0);
        filesCard.addView(st);
        root.addView(filesCard);

        Button batteryShield = UiHelper.makeBtn(this, "🛡️ Enable Zero-Drop Battery Shield", "#151E32", "#00F5D4");
        batteryShield.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception ignored) {}
        });
        root.addView(batteryShield);

        scroll.addView(root);
        return scroll;
    }

    private View buildBentoWebTab() {
        webEngine.startWebServer();
        String url = "http://" + MultiTransferEngine.getLocalIp(this) + ":" + MultiTransferEngine.WEB_PORT;

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(35, 45, 35, 45);

        root.addView(UiHelper.makeTitle(this, "📺 Smart LED TV / PC / Laptop Hub", "#38BDF8", 21f));

        LinearLayout qrBento = new LinearLayout(this);
        qrBento.setOrientation(LinearLayout.VERTICAL);
        qrBento.setGravity(Gravity.CENTER);
        qrBento.setBackground(UiHelper.bentoBox("#FFFFFF", "#38BDF8", 40, 6));
        qrBento.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(-2, -2);
        qLp.topMargin = 30;
        qrBento.setLayoutParams(qLp);

        ImageView qr = new ImageView(this);
        qr.setImageBitmap(QrGenerator.createAutoQr(url));
        qrBento.addView(qr);
        root.addView(qrBento);

        LinearLayout urlCard = new LinearLayout(this);
        urlCard.setOrientation(LinearLayout.VERTICAL);
        urlCard.setBackground(UiHelper.bentoBox("#11192E", "#00F5D4", 35, 2));
        urlCard.setPadding(35, 30, 35, 30);
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(-1, -2);
        uLp.topMargin = 30;
        urlCard.setLayoutParams(uLp);

        urlCard.addView(UiHelper.makeTitle(this, "🌐 Open Browser on LED TV, PC or Laptop:", "#94A3B8", 14f));
        urlCard.addView(UiHelper.makeTitle(this, url, "#00F5D4", 20f));
        TextView hint = new TextView(this);
        hint.setText("All selected Videos, Photos, Songs & APKs can be downloaded directly with 1-Click!");
        hint.setTextColor(Color.WHITE);
        hint.setPadding(0, 10, 0, 0);
        urlCard.addView(hint);
        root.addView(urlCard);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout createHeroBento(String title, String sub, String c1, String c2) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(UiHelper.gradientBento(c1, c2, 42));
        card.setPadding(30, 65, 30, 65);
        UiHelper.enableTvFocus(card);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView t1 = UiHelper.makeTitle(this, title, "#070B14", 22f);
        t1.setGravity(Gravity.CENTER);
        TextView t2 = new TextView(this);
        t2.setText(sub);
        t2.setTextColor(Color.parseColor("#0F172A"));
        t2.setTypeface(null, Typeface.BOLD);
        t2.setGravity(Gravity.CENTER);
        t2.setPadding(0, 10, 0, 0);

        card.addView(t1);
        card.addView(t2);
        return card;
    }

    private LinearLayout createMiniBento(String title, String sub, String accentHex, String tabName) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.bentoBox("#11192E", accentHex, 34, 2));
        card.setPadding(30, 35, 30, 35);
        UiHelper.enableTvFocus(card);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        card.addView(UiHelper.makeTitle(this, title, accentHex, 17f));
        TextView tvSub = new TextView(this);
        tvSub.setText(sub);
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, 8, 0, 0);
        card.addView(tvSub);

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, FilePickerActivity.class);
            i.putExtra("START_TAB", tabName);
            startActivity(i);
        });
        return card;
    }

    private LinearLayout createStatBento(String label, String val, String colorHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiHelper.bentoBox("#11192E", colorHex, 34, 2));
        card.setPadding(30, 35, 30, 35);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        card.addView(UiHelper.makeTitle(this, label, "#94A3B8", 13f));
        card.addView(UiHelper.makeTitle(this, val, colorHex, 22f));
        return card;
    }

    private void shareAppLink() {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, "Download HyperShare X - Works on Android 5 to 16, Smart LED TV, PC & Phones!");
        startActivity(Intent.createChooser(s, "Invite via"));
    }
}
