package com.proshare.app;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FilePickerActivity extends AppCompatActivity {

    private LinearLayout listContainer, subToggleRow;
    private Button btnSelectedCount, btnNextSend;
    private EditText etSearch;
    private final List<AppSession.ShareItem> currentItems = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private String currentTab = "FILES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("START_TAB") != null) {
            currentTab = getIntent().getStringExtra("START_TAB");
        }
        requestPerms();
        setContentView(buildSmoothPickerUi());
        switchCategory(currentTab);
    }

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO
            }, 101);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 101);
        }
    }

    private View buildSmoothPickerUi() {
        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setBackgroundColor(Color.WHITE);

        // 1. Top Bar: ← Send + Rounded Search Pill (Video 00:06)
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(35, 35, 35, 20);

        TextView tvBack = UiHelper.makeTitle(this, "←  Send   ", "#0F172A", 20f);
        UiHelper.attachSmoothBounce(tvBack);
        tvBack.setOnClickListener(v -> finish());

        etSearch = new EditText(this);
        etSearch.setHint("🔍 Search files, videos, apps...");
        etSearch.setHintTextColor(Color.parseColor("#94A3B8"));
        etSearch.setTextColor(Color.parseColor("#0F172A"));
        etSearch.setTextSize(14f);
        etSearch.setBackground(UiHelper.box("#F1F5F9", 50));
        etSearch.setPadding(40, 22, 40, 22);
        etSearch.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderFastList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        topBar.addView(tvBack);
        topBar.addView(etSearch);
        master.addView(topBar);

        // 2. Smooth Horizontal Tabs: History | Apps | Files | Videos | Photos | Songs (Video 00:07)
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(25, 10, 25, 15);

        String[] tabs = {"HISTORY", "APPS", "FILES", "VIDEOS", "PHOTOS", "SONGS"};
        for (String t : tabs) {
            Button b = new Button(this);
            b.setText(t);
            b.setAllCaps(false);
            b.setTextSize(13f);
            b.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, 100);
            lp.setMargins(8, 0, 8, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> switchCategory(t));
            tabButtons.add(b);
            tabRow.addView(b);
        }
        tabScroll.addView(tabRow);
        master.addView(tabScroll);

        // 3. Green Pill Sub-Toggle: [ Installed | Packages ] or [ Received | Send ] (Video 00:06 & 00:07)
        subToggleRow = new LinearLayout(this);
        subToggleRow.setOrientation(LinearLayout.HORIZONTAL);
        subToggleRow.setGravity(Gravity.CENTER);
        subToggleRow.setPadding(50, 15, 50, 15);
        master.addView(subToggleRow);

        // 4. Instant Makkhan-Smooth File List
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(35, 10, 35, 30);
        scroll.addView(listContainer);
        master.addView(scroll);

        // 5. Bottom Selection Bar: [ 0 SELECTED ]  [ NEXT / SEND ] (Video 00:08)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(35, 22, 35, 22);
        bottomBar.setBackground(UiHelper.bentoBox("#FFFFFF", "#E2E8F0", 0, 2));

        btnSelectedCount = new Button(this);
        btnSelectedCount.setText("0 SELECTED");
        btnSelectedCount.setAllCaps(false);
        btnSelectedCount.setTextColor(Color.parseColor("#64748B"));
        btnSelectedCount.setBackground(UiHelper.bentoBox("#FFFFFF", "#CBD5E1", 60, 2));
        btnSelectedCount.setLayoutParams(new LinearLayout.LayoutParams(0, 130, 1f));

        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(30, 10));

        btnNextSend = new Button(this);
        btnNextSend.setText("NEXT");
        btnNextSend.setAllCaps(false);
        btnNextSend.setTextColor(Color.WHITE);
        btnNextSend.setTypeface(null, Typeface.BOLD);
        btnNextSend.setBackground(UiHelper.box("#16CC3E", 60));
        btnNextSend.setLayoutParams(new LinearLayout.LayoutParams(0, 130, 1f));
        UiHelper.attachSmoothBounce(btnNextSend);
        btnNextSend.setOnClickListener(v -> {
            if (AppSession.selectedQueue.isEmpty()) {
                Toast.makeText(this, "Please select at least 1 item to share!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "SEND");
            startActivity(i);
        });

        bottomBar.addView(btnSelectedCount);
        bottomBar.addView(gap);
        bottomBar.addView(btnNextSend);
        master.addView(bottomBar);

        updateBottomBar();
        return master;
    }

    private void switchCategory(String tab) {
        currentTab = tab;
        for (Button b : tabButtons) {
            boolean active = b.getText().toString().equals(tab);
            b.setTextColor(Color.parseColor(active ? "#16CC3E" : "#475569"));
            b.setBackground(UiHelper.box(active ? "#E8F8EE" : "#F8FAFC", 40));
        }

        // Update Green Pill Toggle (Installed/Packages or Received/Send)
        subToggleRow.removeAllViews();
        String leftLabel = "APPS".equals(tab) ? "Installed" : ("HISTORY".equals(tab) ? "Received" : "All " + tab);
        String rightLabel = "APPS".equals(tab) ? "Packages" : ("HISTORY".equals(tab) ? "Send" : "Select All");

        Button bLeft = new Button(this);
        bLeft.setText(leftLabel);
        bLeft.setAllCaps(false);
        bLeft.setTextColor(Color.WHITE);
        bLeft.setBackground(UiHelper.box("#16CC3E", 45));
        bLeft.setLayoutParams(new LinearLayout.LayoutParams(0, 95, 1f));

        Button bRight = new Button(this);
        bRight.setText(rightLabel);
        bRight.setAllCaps(false);
        bRight.setTextColor(Color.parseColor("#16CC3E"));
        bRight.setBackground(UiHelper.bentoBox("#FFFFFF", "#16CC3E", 45, 2));
        bRight.setLayoutParams(new LinearLayout.LayoutParams(0, 95, 1f));
        bRight.setOnClickListener(v -> {
            for (AppSession.ShareItem item : currentItems) {
                if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
            }
            renderFastList(etSearch.getText().toString());
            updateBottomBar();
        });

        subToggleRow.addView(bLeft);
        subToggleRow.addView(bRight);

        // Load instantaneously from RAM Cache, or fetch on background thread
        String queryTab = "HISTORY".equals(tab) ? "FILES" : tab;
        if (AppSession.ramCache.containsKey(queryTab)) {
            currentItems.clear();
            currentItems.addAll(AppSession.ramCache.get(queryTab));
            renderFastList(etSearch.getText().toString());
        } else {
            Executors.newSingleThreadExecutor().execute(() -> {
                List<AppSession.ShareItem> loaded = AppSession.getCategoryItems(this, queryTab);
                runOnUiThread(() -> {
                    currentItems.clear();
                    currentItems.addAll(loaded);
                    renderFastList(etSearch.getText().toString());
                });
            });
        }
    }

    private void renderFastList(String query) {
        listContainer.removeAllViews();
        String q = query.toLowerCase().trim();

        for (AppSession.ShareItem item : currentItems) {
            if (!q.isEmpty() && !item.name.toLowerCase().contains(q)) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(20, 22, 20, 22);
            row.setBackground(UiHelper.bentoBox("#FFFFFF", "#F1F5F9", 28, 2));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
            rLp.bottomMargin = 14;
            row.setLayoutParams(rLp);

            // Clean File Icon Badge (Like Yellow File Icon in Video 00:08)
            TextView iconBadge = UiHelper.makeTitle(this, "APPS".equals(item.category) ? "📦" : ("VIDEOS".equals(item.category) ? "🎬" : "📁"), "#FFFFFF", 18f);
            iconBadge.setGravity(Gravity.CENTER);
            iconBadge.setBackground(UiHelper.box("#FACC15", 22));
            iconBadge.setLayoutParams(new LinearLayout.LayoutParams(105, 105));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(28, 0, 15, 0);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView tvName = UiHelper.makeTitle(this, item.name, "#0F172A", 14.5f);
            tvName.setMaxLines(1);
            TextView tvSize = new TextView(this);
            tvSize.setText(AppSession.formatSize(item.size));
            tvSize.setTextColor(Color.parseColor("#94A3B8"));
            tvSize.setTextSize(12.5f);
            textCol.addView(tvName);
            textCol.addView(tvSize);

            CheckBox cb = new CheckBox(this);
            cb.setChecked(AppSession.selectedQueue.contains(item));
            cb.setOnCheckedChangeListener((b, checked) -> {
                if (checked) {
                    if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
                } else {
                    AppSession.selectedQueue.remove(item);
                }
                updateBottomBar();
            });

            row.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
            row.addView(iconBadge);
            row.addView(textCol);
            row.addView(cb);
            listContainer.addView(row);
        }
    }

    private void updateBottomBar() {
        int count = AppSession.selectedQueue.size();
        btnSelectedCount.setText(count + " SELECTED");
        btnSelectedCount.setTextColor(Color.parseColor(count > 0 ? "#16CC3E" : "#64748B"));
        btnNextSend.setText(count > 0 ? "SEND" : "NEXT");
    }
}
