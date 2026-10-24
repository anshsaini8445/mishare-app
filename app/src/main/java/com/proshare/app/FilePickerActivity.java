package com.proshare.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilePickerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FastFileAdapter adapter;
    private LinearLayout emptyStateBox;
    private Button btnSelectedCount, btnNextSend;
    private EditText etSearch;
    private final List<AppSession.ShareItem> currentItems = new ArrayList<>();
    private final List<AppSession.ShareItem> filteredItems = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private final Map<String, Drawable> iconCache = new HashMap<>();
    private final ExecutorService iconLoaderPool = Executors.newFixedThreadPool(4);
    private String currentTab = "VIDEOS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("START_TAB") != null) {
            currentTab = getIntent().getStringExtra("START_TAB");
        }
        requestPerms();
        setContentView(ThemeEngine.applyOneHandWrapper(this, buildZeroHangRecyclerUi()));
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

    private View buildZeroHangRecyclerUi() {
        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setBackgroundColor(Color.parseColor(ThemeEngine.bg(this)));

        // 1. Top Search & Back Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(35, 35, 35, 20);

        TextView tvBack = UiHelper.makeTitle(this, "←  " + LangEngine.t(this, "SEND") + "  ", ThemeEngine.textMain(this), 19f);
        UiHelper.attachSmoothBounce(tvBack);
        tvBack.setOnClickListener(v -> finish());

        etSearch = new EditText(this);
        etSearch.setHint("🔍 Search videos, apps, songs, files...");
        etSearch.setHintTextColor(Color.parseColor(ThemeEngine.textSub(this)));
        etSearch.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        etSearch.setTextSize(14f);
        etSearch.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 50, 2));
        etSearch.setPadding(40, 22, 40, 22);
        etSearch.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        topBar.addView(tvBack);
        topBar.addView(etSearch);
        master.addView(topBar);

        // 2. Category Tabs: HISTORY | APPS | VIDEOS | PHOTOS | SONGS | FILES
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(25, 10, 25, 15);

        String[] tabs = {"HISTORY", "APPS", "VIDEOS", "PHOTOS", "SONGS", "FILES"};
        for (String t : tabs) {
            Button b = new Button(this);
            b.setText(t);
            b.setAllCaps(false);
            b.setTextSize(12.5f);
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

        // 3. Quick Action Strip: Select All + Auto-Zip Indicator
        LinearLayout subRow = new LinearLayout(this);
        subRow.setOrientation(LinearLayout.HORIZONTAL);
        subRow.setGravity(Gravity.CENTER_VERTICAL);
        subRow.setPadding(35, 5, 35, 10);

        Button btnSelectAll = new Button(this);
        btnSelectAll.setText("☑ Select All in Tab");
        btnSelectAll.setAllCaps(false);
        btnSelectAll.setTextColor(Color.parseColor(ThemeEngine.accent(this)));
        btnSelectAll.setBackgroundColor(Color.TRANSPARENT);
        btnSelectAll.setOnClickListener(v -> {
            for (AppSession.ShareItem item : filteredItems) {
                if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
            }
            adapter.notifyDataSetChanged();
            updateBottomBar();
        });

        Button btnClear = new Button(this);
        btnClear.setText("✖ Clear");
        btnClear.setAllCaps(false);
        btnClear.setTextColor(Color.parseColor(ThemeEngine.textSub(this)));
        btnClear.setBackgroundColor(Color.TRANSPARENT);
        btnClear.setOnClickListener(v -> {
            AppSession.selectedQueue.clear();
            adapter.notifyDataSetChanged();
            updateBottomBar();
        });

        subRow.addView(btnSelectAll);
        subRow.addView(btnClear);
        master.addView(subRow);

        // 4. Empty Robot Illustration Box (Shown when History or Tab is Empty)
        emptyStateBox = new LinearLayout(this);
        emptyStateBox.setOrientation(LinearLayout.VERTICAL);
        emptyStateBox.setGravity(Gravity.CENTER);
        emptyStateBox.setVisibility(View.GONE);
        UiHelper.EmptyRobotRadarView robot = new UiHelper.EmptyRobotRadarView(this);
        robot.setLayoutParams(new LinearLayout.LayoutParams(240, 220));
        emptyStateBox.addView(robot);
        emptyStateBox.addView(UiHelper.makeTitle(this, "No files in this tab yet!", ThemeEngine.textSub(this), 15f));
        master.addView(emptyStateBox);

        // 5. ZERO-HANG RECYCLERVIEW (Lazy Loads Only Visible Rows at 120 FPS!)
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        adapter = new FastFileAdapter();
        recyclerView.setAdapter(adapter);
        master.addView(recyclerView);

        // 6. Bottom Bar: [ X SELECTED ]  [ TURBO SEND ➔ ]
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(35, 22, 35, 22);
        bottomBar.setBackground(UiHelper.bentoBox(ThemeEngine.card(this), ThemeEngine.accent(this), 0, 2));

        btnSelectedCount = new Button(this);
        btnSelectedCount.setText("0 SELECTED");
        btnSelectedCount.setAllCaps(false);
        btnSelectedCount.setTextColor(Color.parseColor(ThemeEngine.textMain(this)));
        btnSelectedCount.setBackground(UiHelper.bentoBox(ThemeEngine.bg(this), ThemeEngine.accent(this), 60, 2));
        btnSelectedCount.setLayoutParams(new LinearLayout.LayoutParams(0, 130, 1f));

        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(25, 10));

        btnNextSend = new Button(this);
        btnNextSend.setText("TURBO SEND ➔");
        btnNextSend.setAllCaps(false);
        btnNextSend.setTextColor(Color.parseColor("#070B14"));
        btnNextSend.setTypeface(null, Typeface.BOLD);
        btnNextSend.setBackground(UiHelper.gradientBento("#00F5D4", "#10B981", 60));
        btnNextSend.setLayoutParams(new LinearLayout.LayoutParams(0, 130, 1f));
        UiHelper.attachSmoothBounce(btnNextSend);
        btnNextSend.setOnClickListener(v -> {
            if (AppSession.selectedQueue.isEmpty()) {
                Toast.makeText(this, "Select at least 1 file to share!", Toast.LENGTH_SHORT).show();
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
            b.setTextColor(Color.parseColor(active ? "#070B14" : ThemeEngine.textMain(this)));
            b.setBackground(UiHelper.box(active ? ThemeEngine.accent(this) : ThemeEngine.card(this), 40));
        }

        iconLoaderPool.execute(() -> {
            List<AppSession.ShareItem> loaded = AppSession.getCategoryItems(this, tab);
            runOnUiThread(() -> {
                currentItems.clear();
                currentItems.addAll(loaded);
                filterList(etSearch.getText().toString());
            });
        });
    }

    private void filterList(String q) {
        String query = q.toLowerCase().trim();
        filteredItems.clear();
        for (AppSession.ShareItem item : currentItems) {
            if (query.isEmpty() || item.name.toLowerCase().contains(query)) {
                filteredItems.add(item);
            }
        }
        emptyStateBox.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void updateBottomBar() {
        int count = AppSession.selectedQueue.size();
        long bytes = AppSession.getSelectedTotalBytes();
        btnSelectedCount.setText(count + " (" + AppSession.formatSize(bytes) + ")");
    }

    // RECYCLERVIEW ADAPTER WITH ASYNC ICON LOADING (ZERO UI BLOCKING!)
    private class FastFileAdapter extends RecyclerView.Adapter<FastFileAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            LinearLayout row;
            ImageView iconView;
            TextView tvName, tvSub;
            Button btnPlay;
            CheckBox cb;

            VH(LinearLayout itemView) {
                super(itemView);
                this.row = itemView;
                this.iconView = (ImageView) itemView.getChildAt(0);
                LinearLayout col = (LinearLayout) itemView.getChildAt(1);
                this.tvName = (TextView) col.getChildAt(0);
                this.tvSub = (TextView) col.getChildAt(1);
                this.btnPlay = (Button) itemView.getChildAt(2);
                this.cb = (CheckBox) itemView.getChildAt(3);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(FilePickerActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(25, 20, 25, 20);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
            lp.setMargins(25, 8, 25, 8);
            row.setLayoutParams(lp);

            ImageView iv = new ImageView(FilePickerActivity.this);
            iv.setLayoutParams(new LinearLayout.LayoutParams(105, 105));
            iv.setBackground(UiHelper.box("#2563EB", 24));
            iv.setPadding(12, 12, 12, 12);

            LinearLayout textCol = new LinearLayout(FilePickerActivity.this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(25, 0, 15, 0);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView tvName = UiHelper.makeTitle(FilePickerActivity.this, "", ThemeEngine.textMain(FilePickerActivity.this), 14f);
            tvName.setMaxLines(1);
            TextView tvSub = new TextView(FilePickerActivity.this);
            tvSub.setTextSize(12f);
            textCol.addView(tvName);
            textCol.addView(tvSub);

            Button btnPlay = new Button(FilePickerActivity.this);
            btnPlay.setText("▶️");
            btnPlay.setBackground(UiHelper.box("#1E293B", 25));
            btnPlay.setTextColor(Color.WHITE);
            btnPlay.setLayoutParams(new LinearLayout.LayoutParams(95, 90));

            CheckBox cb = new CheckBox(FilePickerActivity.this);

            row.addView(iv);
            row.addView(textCol);
            row.addView(btnPlay);
            row.addView(cb);
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final AppSession.ShareItem item = filteredItems.get(position);
            boolean isSel = AppSession.selectedQueue.contains(item);

            h.row.setBackground(UiHelper.bentoBox(ThemeEngine.card(FilePickerActivity.this),
                    isSel ? ThemeEngine.accent(FilePickerActivity.this) : "#1E293B", 28, isSel ? 3 : 1));
            h.tvName.setText(item.name);
            h.tvName.setTextColor(Color.parseColor(ThemeEngine.textMain(FilePickerActivity.this)));
            h.tvSub.setText(AppSession.formatSize(item.size) + "  •  " + item.dateStr);
            h.tvSub.setTextColor(Color.parseColor(ThemeEngine.textSub(FilePickerActivity.this)));

            // Async Lazy Icon Loader for APKs so scrolling never stutters
            h.iconView.setTag(item.path);
            if (iconCache.containsKey(item.path)) {
                h.iconView.setImageDrawable(iconCache.get(item.path));
            } else {
                h.iconView.setImageDrawable(null);
                if ("APPS".equals(item.category)) {
                    iconLoaderPool.execute(() -> {
                        try {
                            PackageManager pm = getPackageManager();
                            PackageInfo pi = pm.getPackageArchiveInfo(item.path, 0);
                            if (pi != null && pi.applicationInfo != null) {
                                pi.applicationInfo.sourceDir = item.path;
                                pi.applicationInfo.publicSourceDir = item.path;
                                final Drawable d = pi.applicationInfo.loadIcon(pm);
                                if (d != null) {
                                    iconCache.put(item.path, d);
                                    runOnUiThread(() -> {
                                        if (item.path.equals(h.iconView.getTag())) {
                                            h.iconView.setImageDrawable(d);
                                        }
                                    });
                                }
                            }
                        } catch (Exception ignored) {}
                    });
                }
            }

            // In-App Media Player Button
            h.btnPlay.setOnClickListener(v -> SuperToolsDialog.openInAppMediaPlayer(FilePickerActivity.this, item));

            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(isSel);
            h.cb.setOnCheckedChangeListener((b, checked) -> {
                HapticSoundEngine.tick(b);
                if (checked) {
                    if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
                } else {
                    AppSession.selectedQueue.remove(item);
                }
                h.row.setBackground(UiHelper.bentoBox(ThemeEngine.card(FilePickerActivity.this),
                        checked ? ThemeEngine.accent(FilePickerActivity.this) : "#1E293B", 28, checked ? 3 : 1));
                updateBottomBar();
            });

            h.row.setOnClickListener(v -> h.cb.setChecked(!h.cb.isChecked()));
        }

        @Override
        public int getItemCount() {
            return filteredItems.size();
        }
    }
}
