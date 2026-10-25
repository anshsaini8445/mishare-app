package com.proshare.app;

import android.Manifest;
import android.content.Intent;
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
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilePickerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SmartMediaAdapter adapter;
    private LinearLayout subToggleRow, emptyBox;
    private Button btnSelectedCount, btnNextSend;
    private EditText etSearch;

    private final List<AppSession.ShareItem> currentItems = new ArrayList<>();
    private final List<AppSession.ShareItem> filteredItems = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private static final Map<String, Drawable> appIconCache = new ConcurrentHashMap<>();
    private final ExecutorService loaderPool = Executors.newFixedThreadPool(4);
    private String currentTab = "VIDEOS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("START_TAB") != null) {
            currentTab = getIntent().getStringExtra("START_TAB");
        }
        requestMediaPermissions();
        setContentView(buildPickerLayout());
        switchTab(currentTab);
    }

    private void requestMediaPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_CONTACTS
            }, 101);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CONTACTS
            }, 101);
        }
    }

    private View buildPickerLayout() {
        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setBackgroundColor(Color.WHITE);

        // 1. Top Bar: ← Send + Rounded Search Bar
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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applySearchFilter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        topBar.addView(tvBack);
        topBar.addView(etSearch);
        master.addView(topBar);

        // 2. Clean Category Tabs: History | Apps | Files | Videos | Photos | Songs | Contacts
        HorizontalScrollView tabScroll = new HorizontalScrollView(this);
        tabScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(25, 8, 25, 12);

        String[] tabs = {"HISTORY", "APPS", "FILES", "VIDEOS", "PHOTOS", "SONGS", "CONTACTS"};
        for (final String t : tabs) {
            Button b = new Button(this);
            b.setText(t.charAt(0) + t.substring(1).toLowerCase());
            b.setTag(t);
            b.setAllCaps(false);
            b.setTextSize(13.5f);
            b.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, 96);
            lp.setMargins(8, 0, 8, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> switchTab(t));
            tabButtons.add(b);
            tabRow.addView(b);
        }
        tabScroll.addView(tabRow);
        master.addView(tabScroll);

        // 3. Green Pill Sub-Bar (Installed / Select All)
        subToggleRow = new LinearLayout(this);
        subToggleRow.setOrientation(LinearLayout.HORIZONTAL);
        subToggleRow.setGravity(Gravity.CENTER);
        subToggleRow.setPadding(45, 10, 45, 15);
        master.addView(subToggleRow);

        // 4. Empty State Message
        emptyBox = new LinearLayout(this);
        emptyBox.setOrientation(LinearLayout.VERTICAL);
        emptyBox.setGravity(Gravity.CENTER);
        emptyBox.setPadding(40, 90, 40, 90);
        emptyBox.setVisibility(View.GONE);
        emptyBox.addView(UiHelper.makeTitle(this, "📂 No items found in this category", "#94A3B8", 15f));
        master.addView(emptyBox);

        // 5. RecyclerView with Dynamic Grid/List LayoutManager
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        recyclerView.setPadding(20, 8, 20, 20);
        recyclerView.setClipToPadding(false);
        adapter = new SmartMediaAdapter();
        recyclerView.setAdapter(adapter);
        master.addView(recyclerView);

        // 6. Floating Bottom Bar: [ 0 SELECTED ]  [ NEXT / SEND ]
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(35, 22, 35, 22);
        bottomBar.setBackground(UiHelper.bentoBox("#FFFFFF", "#E2E8F0", 0, 2));

        btnSelectedCount = new Button(this);
        btnSelectedCount.setText("0 SELECTED");
        btnSelectedCount.setAllCaps(false);
        btnSelectedCount.setTextColor(Color.parseColor("#64748B"));
        btnSelectedCount.setBackground(UiHelper.bentoBox("#FFFFFF", "#CBD5E1", 60, 2));
        btnSelectedCount.setLayoutParams(new LinearLayout.LayoutParams(0, 128, 1f));
        btnSelectedCount.setOnClickListener(v -> {
            AppSession.selectedQueue.clear();
            adapter.notifyDataSetChanged();
            updateBottomBar();
        });

        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(28, 10));

        btnNextSend = new Button(this);
        btnNextSend.setText("NEXT");
        btnNextSend.setAllCaps(false);
        btnNextSend.setTextColor(Color.WHITE);
        btnNextSend.setTypeface(null, Typeface.BOLD);
        btnNextSend.setBackground(UiHelper.box("#16CC3E", 60));
        btnNextSend.setLayoutParams(new LinearLayout.LayoutParams(0, 128, 1f));
        UiHelper.attachSmoothBounce(btnNextSend);
        btnNextSend.setOnClickListener(v -> {
            if (AppSession.selectedQueue.isEmpty()) {
                Toast.makeText(this, "Please select at least 1 file first!", Toast.LENGTH_SHORT).show();
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

    private void switchTab(final String tab) {
        currentTab = tab;
        for (Button b : tabButtons) {
            boolean active = tab.equals(b.getTag());
            b.setTextColor(Color.parseColor(active ? "#16CC3E" : "#475569"));
            b.setBackground(UiHelper.box(active ? "#E8F8EE" : "#F8FAFC", 40));
        }

        // Configure Smart Grid Columns: 4 columns for Apps, 3 columns for Videos/Photos, 1 column for Songs/Files
        if ("APPS".equals(tab)) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        } else if ("VIDEOS".equals(tab) || "PHOTOS".equals(tab)) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        // Update Green Sub-Toggle Bar
        subToggleRow.removeAllViews();
        String leftText = "APPS".equals(tab) ? "Installed Apps" : ("HISTORY".equals(tab) ? "Recent Transfers" : "All " + tab);
        Button bLeft = new Button(this);
        bLeft.setText(leftText);
        bLeft.setAllCaps(false);
        bLeft.setTextColor(Color.WHITE);
        bLeft.setBackground(UiHelper.box("#16CC3E", 45));
        bLeft.setLayoutParams(new LinearLayout.LayoutParams(0, 92, 1f));

        Button bRight = new Button(this);
        bRight.setText("Select All");
        bRight.setAllCaps(false);
        bRight.setTextColor(Color.parseColor("#16CC3E"));
        bRight.setBackground(UiHelper.bentoBox("#FFFFFF", "#16CC3E", 45, 2));
        bRight.setLayoutParams(new LinearLayout.LayoutParams(0, 92, 1f));
        bRight.setOnClickListener(v -> {
            for (AppSession.ShareItem item : filteredItems) {
                if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
            }
            adapter.notifyDataSetChanged();
            updateBottomBar();
        });

        subToggleRow.addView(bLeft);
        subToggleRow.addView(bRight);

        loaderPool.execute(() -> {
            final List<AppSession.ShareItem> loaded = AppSession.getCategoryItems(FilePickerActivity.this, tab);
            runOnUiThread(() -> {
                currentItems.clear();
                currentItems.addAll(loaded);
                applySearchFilter(etSearch.getText().toString());
            });
        });
    }

    private void applySearchFilter(String q) {
        String query = q.toLowerCase().trim();
        filteredItems.clear();
        for (AppSession.ShareItem it : currentItems) {
            if (query.isEmpty() || it.name.toLowerCase().contains(query)) {
                filteredItems.add(it);
            }
        }
        emptyBox.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void updateBottomBar() {
        int count = AppSession.selectedQueue.size();
        long totalBytes = AppSession.getSelectedTotalBytes();
        if (count > 0) {
            btnSelectedCount.setText(count + " (" + AppSession.formatSize(totalBytes) + ")");
            btnSelectedCount.setTextColor(Color.parseColor("#16CC3E"));
            btnNextSend.setText("SEND (" + count + ")");
        } else {
            btnSelectedCount.setText("0 SELECTED");
            btnSelectedCount.setTextColor(Color.parseColor("#64748B"));
            btnNextSend.setText("NEXT");
        }
    }

    // MULTI-VIEW RECYCLERVIEW ADAPTER: GRID FOR APPS/VIDEOS/PHOTOS + LIST FOR SONGS/FILES
    private class SmartMediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_APP_GRID = 1;
        private static final int TYPE_MEDIA_GRID = 2;
        private static final int TYPE_FILE_LIST = 3;

        @Override
        public int getItemViewType(int position) {
            if ("APPS".equals(currentTab)) return TYPE_APP_GRID;
            if ("VIDEOS".equals(currentTab) || "PHOTOS".equals(currentTab)) return TYPE_MEDIA_GRID;
            return TYPE_FILE_LIST;
        }

        class AppGridVH extends RecyclerView.ViewHolder {
            LinearLayout card;
            ImageView icon;
            TextView name, size;
            CheckBox cb;

            AppGridVH(LinearLayout v) {
                super(v);
                card = v;
                icon = (ImageView) v.getChildAt(0);
                name = (TextView) v.getChildAt(1);
                size = (TextView) v.getChildAt(2);
                cb = (CheckBox) v.getChildAt(3);
            }
        }

        class MediaGridVH extends RecyclerView.ViewHolder {
            FrameLayout box;
            ImageView thumb;
            TextView badgeDuration, tvTitle;
            CheckBox cb;

            MediaGridVH(LinearLayout wrapper) {
                super(wrapper);
                box = (FrameLayout) wrapper.getChildAt(0);
                thumb = (ImageView) box.getChildAt(0);
                badgeDuration = (TextView) box.getChildAt(1);
                cb = (CheckBox) box.getChildAt(2);
                tvTitle = (TextView) wrapper.getChildAt(1);
            }
        }

        class FileListVH extends RecyclerView.ViewHolder {
            LinearLayout row;
            TextView badgeIcon, tvName, tvSub;
            Button btnPlay;
            CheckBox cb;

            FileListVH(LinearLayout r) {
                super(r);
                row = r;
                badgeIcon = (TextView) r.getChildAt(0);
                LinearLayout col = (LinearLayout) r.getChildAt(1);
                tvName = (TextView) col.getChildAt(0);
                tvSub = (TextView) col.getChildAt(1);
                btnPlay = (Button) r.getChildAt(2);
                cb = (CheckBox) r.getChildAt(3);
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_APP_GRID) {
                LinearLayout card = new LinearLayout(FilePickerActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_HORIZONTAL);
                card.setPadding(12, 20, 12, 16);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
                lp.setMargins(8, 8, 8, 8);
                card.setLayoutParams(lp);

                ImageView icon = new ImageView(FilePickerActivity.this);
                icon.setLayoutParams(new LinearLayout.LayoutParams(125, 125));

                TextView name = UiHelper.makeTitle(FilePickerActivity.this, "", "#0F172A", 12f);
                name.setMaxLines(1);
                name.setGravity(Gravity.CENTER);
                name.setPadding(0, 10, 0, 2);

                TextView size = new TextView(FilePickerActivity.this);
                size.setTextSize(11f);
                size.setTextColor(Color.parseColor("#64748B"));
                size.setGravity(Gravity.CENTER);

                CheckBox cb = new CheckBox(FilePickerActivity.this);

                card.addView(icon);
                card.addView(name);
                card.addView(size);
                card.addView(cb);
                return new AppGridVH(card);
            } else if (viewType == TYPE_MEDIA_GRID) {
                LinearLayout wrapper = new LinearLayout(FilePickerActivity.this);
                wrapper.setOrientation(LinearLayout.VERTICAL);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
                lp.setMargins(8, 8, 8, 12);
                wrapper.setLayoutParams(lp);

                FrameLayout box = new FrameLayout(FilePickerActivity.this);
                box.setLayoutParams(new LinearLayout.LayoutParams(-1, 290));

                ImageView thumb = new ImageView(FilePickerActivity.this);
                thumb.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setBackgroundColor(Color.parseColor("#E2E8F0"));

                TextView badge = new TextView(FilePickerActivity.this);
                badge.setTextSize(10.5f);
                badge.setTextColor(Color.WHITE);
                badge.setBackground(UiHelper.box("#99000000", 14));
                badge.setPadding(14, 4, 14, 4);
                FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(-2, -2);
                bLp.gravity = Gravity.BOTTOM | Gravity.START;
                bLp.setMargins(10, 10, 10, 10);
                badge.setLayoutParams(bLp);

                CheckBox cb = new CheckBox(FilePickerActivity.this);
                FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(-2, -2);
                cLp.gravity = Gravity.TOP | Gravity.END;
                cb.setLayoutParams(cLp);

                box.addView(thumb);
                box.addView(badge);
                box.addView(cb);

                TextView title = new TextView(FilePickerActivity.this);
                title.setMaxLines(1);
                title.setTextSize(11.5f);
                title.setTextColor(Color.parseColor("#0F172A"));
                title.setPadding(4, 6, 4, 2);

                wrapper.addView(box);
                wrapper.addView(title);
                return new MediaGridVH(wrapper);
            } else {
                LinearLayout row = new LinearLayout(FilePickerActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(20, 20, 20, 20);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
                lp.setMargins(10, 6, 10, 6);
                row.setLayoutParams(lp);

                TextView badge = UiHelper.makeTitle(FilePickerActivity.this, "FILE", "#FFFFFF", 12f);
                badge.setGravity(Gravity.CENTER);
                badge.setLayoutParams(new LinearLayout.LayoutParams(105, 105));

                LinearLayout col = new LinearLayout(FilePickerActivity.this);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setPadding(24, 0, 14, 0);
                col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

                TextView tvName = UiHelper.makeTitle(FilePickerActivity.this, "", "#0F172A", 14f);
                tvName.setMaxLines(1);
                TextView tvSub = new TextView(FilePickerActivity.this);
                tvSub.setTextSize(12f);
                tvSub.setTextColor(Color.parseColor("#64748B"));
                col.addView(tvName);
                col.addView(tvSub);

                Button btnPlay = new Button(FilePickerActivity.this);
                btnPlay.setText("▶");
                btnPlay.setTextColor(Color.parseColor("#16CC3E"));
                btnPlay.setBackground(UiHelper.box("#E8F8EE", 30));
                btnPlay.setLayoutParams(new LinearLayout.LayoutParams(90, 90));

                CheckBox cb = new CheckBox(FilePickerActivity.this);

                row.addView(badge);
                row.addView(col);
                row.addView(btnPlay);
                row.addView(cb);
                return new FileListVH(row);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final AppSession.ShareItem item = filteredItems.get(position);
            final boolean isSel = AppSession.selectedQueue.contains(item);

            if (holder instanceof AppGridVH) {
                final AppGridVH h = (AppGridVH) holder;
                h.card.setBackground(UiHelper.bentoBox("#FFFFFF", isSel ? "#16CC3E" : "#E2E8F0", 28, isSel ? 4 : 2));
                h.name.setText(item.name.replace(".apk", ""));
                h.size.setText(item.durationOrSub);

                h.icon.setTag(item.path);
                if (appIconCache.containsKey(item.path)) {
                    h.icon.setImageDrawable(appIconCache.get(item.path));
                } else {
                    h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
                    loaderPool.execute(() -> {
                        try {
                            PackageManager pm = getPackageManager();
                            final Drawable d = (item.packageName != null && !item.packageName.isEmpty())
                                    ? pm.getApplicationIcon(item.packageName)
                                    : pm.getApplicationInfo(getPackageName(), 0).loadIcon(pm);
                            if (d != null) {
                                appIconCache.put(item.path, d);
                                runOnUiThread(() -> {
                                    if (item.path.equals(h.icon.getTag())) {
                                        h.icon.setImageDrawable(d);
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    });
                }

                h.cb.setOnCheckedChangeListener(null);
                h.cb.setChecked(isSel);
                h.cb.setOnCheckedChangeListener((b, checked) -> toggleSelection(item, checked, h.card));
                h.card.setOnClickListener(v -> h.cb.setChecked(!h.cb.isChecked()));

            } else if (holder instanceof MediaGridVH) {
                final MediaGridVH h = (MediaGridVH) holder;
                h.box.setBackground(UiHelper.bentoBox("#E2E8F0", isSel ? "#16CC3E" : "#CBD5E1", 20, isSel ? 5 : 1));
                h.tvTitle.setText(item.name);
                h.badgeDuration.setText("VIDEOS".equals(item.category) ? ("▶ " + item.durationOrSub) : item.durationOrSub);

                // Real Video & Photo Thumbnail Loaded Smoothly via Glide
                Glide.with(FilePickerActivity.this)
                        .load(new File(item.path))
                        .centerCrop()
                        .into(h.thumb);

                h.badgeDuration.setOnClickListener(v -> {
                    if ("VIDEOS".equals(item.category)) {
                        SuperToolsDialog.openInAppMediaPlayer(FilePickerActivity.this, item);
                    }
                });

                h.cb.setOnCheckedChangeListener(null);
                h.cb.setChecked(isSel);
                h.cb.setOnCheckedChangeListener((b, checked) -> toggleSelection(item, checked, h.box));
                h.box.setOnClickListener(v -> h.cb.setChecked(!h.cb.isChecked()));

            } else if (holder instanceof FileListVH) {
                final FileListVH h = (FileListVH) holder;
                h.row.setBackground(UiHelper.bentoBox("#FFFFFF", isSel ? "#16CC3E" : "#F1F5F9", 26, isSel ? 4 : 2));
                h.tvName.setText(item.name);
                h.tvSub.setText(AppSession.formatSize(item.size) + "  •  " + item.durationOrSub);

                if ("SONGS".equals(item.category)) {
                    h.badgeIcon.setText("🎵");
                    h.badgeIcon.setBackground(UiHelper.box("#8B5CF6", 24));
                    h.btnPlay.setVisibility(View.VISIBLE);
                    h.btnPlay.setOnClickListener(v -> SuperToolsDialog.openInAppMediaPlayer(FilePickerActivity.this, item));
                } else if ("CONTACTS".equals(item.category)) {
                    h.badgeIcon.setText("👤");
                    h.badgeIcon.setBackground(UiHelper.box("#0EA5E9", 24));
                    h.btnPlay.setVisibility(View.GONE);
                } else {
                    String ext = item.name.contains(".") ? item.name.substring(item.name.lastIndexOf('.') + 1).toUpperCase() : "DOC";
                    if (ext.length() > 4) ext = "FILE";
                    h.badgeIcon.setText(ext);
                    h.badgeIcon.setBackground(UiHelper.box("#F59E0B", 24));
                    h.btnPlay.setVisibility(View.GONE);
                }

                h.cb.setOnCheckedChangeListener(null);
                h.cb.setChecked(isSel);
                h.cb.setOnCheckedChangeListener((b, checked) -> toggleSelection(item, checked, h.row));
                h.row.setOnClickListener(v -> h.cb.setChecked(!h.cb.isChecked()));
            }
        }

        private void toggleSelection(AppSession.ShareItem item, boolean checked, View container) {
            HapticSoundEngine.tick(container);
            if (checked) {
                if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
            } else {
                AppSession.selectedQueue.remove(item);
            }
            container.setBackground(UiHelper.bentoBox("#FFFFFF", checked ? "#16CC3E" : "#E2E8F0", 24, checked ? 4 : 2));
            updateBottomBar();
        }

        @Override
        public int getItemCount() {
            return filteredItems.size();
        }
    }
}
