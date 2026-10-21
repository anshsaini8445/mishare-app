package com.proshare.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilePickerActivity extends AppCompatActivity {

    private LinearLayout listContainer;
    private Button btnSelectedCount;
    private final List<AppSession.ShareItem> currentItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.selectedQueue.clear();
        requestPerms();
        setContentView(buildUi());
        loadData("VIDEOS");
    }

    private void requestPerms() {
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

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0B0F19"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);

        root.addView(UiHelper.makeTitle(this, "📁 Select Videos, Apps & Files", "#00F5D4", 20f));

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, 20, 0, 15);

        String[] tabs = {"VIDEOS", "PHOTOS", "SONGS", "APPS", "FILES"};
        for (String t : tabs) {
            Button b = new Button(this);
            b.setText(t);
            b.setTextSize(11f);
            b.setAllCaps(false);
            b.setTextColor(Color.WHITE);
            b.setBackground(UiHelper.box("#1E293B", 25));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 110, 1f);
            lp.setMargins(4, 0, 4, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> loadData(t));
            tabRow.addView(b);
        }
        root.addView(tabRow);

        Button btnSelectAll = new Button(this);
        btnSelectAll.setText("☑ Select All in List");
        btnSelectAll.setAllCaps(false);
        btnSelectAll.setTextColor(Color.parseColor("#00F5D4"));
        btnSelectAll.setBackgroundColor(Color.TRANSPARENT);
        btnSelectAll.setOnClickListener(v -> {
            for (AppSession.ShareItem item : currentItems) {
                if (!AppSession.selectedQueue.contains(item)) {
                    AppSession.selectedQueue.add(item);
                }
            }
            renderList();
            updateCount();
        });
        root.addView(btnSelectAll);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(0, 30, 0, 10);

        btnSelectedCount = UiHelper.makeBtn(this, "0 SELECTED", "#1E293B", "#94A3B8");
        btnSelectedCount.setLayoutParams(new LinearLayout.LayoutParams(0, 135, 1f));

        Button btnNext = UiHelper.makeBtn(this, "NEXT ➔", "#10B981", "#0B0F19");
        btnNext.setLayoutParams(new LinearLayout.LayoutParams(0, 135, 1f));
        btnNext.setOnClickListener(v -> {
            if (AppSession.selectedQueue.isEmpty()) {
                Toast.makeText(this, "Please select at least 1 file!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "SEND");
            startActivity(i);
        });

        bottomBar.addView(btnSelectedCount);
        bottomBar.addView(btnNext);
        root.addView(bottomBar);

        scroll.addView(root);
        return scroll;
    }

    private void loadData(String tab) {
        currentItems.clear();
        try {
            if ("APPS".equals(tab)) {
                PackageManager pm = getPackageManager();
                for (ApplicationInfo app : pm.getInstalledApplications(0)) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && app.sourceDir != null) {
                        File f = new File(app.sourceDir);
                        currentItems.add(new AppSession.ShareItem(pm.getApplicationLabel(app).toString() + ".apk", f.getAbsolutePath(), f.length(), "APPS"));
                    }
                }
            } else {
                Uri uri = MediaStore.Files.getContentUri("external");
                if ("VIDEOS".equals(tab)) uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                else if ("PHOTOS".equals(tab)) uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("SONGS".equals(tab)) uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE};
                try (Cursor c = getContentResolver().query(uri, proj, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 100) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            if (path != null && new File(path).exists() && size > 0) {
                                currentItems.add(new AppSession.ShareItem(name != null ? name : "File_" + count, path, size, tab));
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        renderList();
    }

    private void renderList() {
        listContainer.removeAllViews();
        for (AppSession.ShareItem item : currentItems) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(UiHelper.box("#161F33", 25));
            row.setPadding(25, 20, 25, 20);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = 15;
            row.setLayoutParams(lp);

            TextView tv = new TextView(this);
            tv.setText(item.name + "\n" + AppSession.formatSize(item.size));
            tv.setTextColor(Color.WHITE);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            CheckBox cb = new CheckBox(this);
            cb.setChecked(AppSession.selectedQueue.contains(item));
            cb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
                } else {
                    AppSession.selectedQueue.remove(item);
                }
                updateCount();
            });

            row.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
            row.addView(tv);
            row.addView(cb);
            listContainer.addView(row);
        }
    }

    private void updateCount() {
        btnSelectedCount.setText(AppSession.selectedQueue.size() + " SELECTED");
        btnSelectedCount.setTextColor(Color.parseColor("#00F5D4"));
    }
}
