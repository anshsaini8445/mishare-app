package com.proshare.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FilePickerActivity extends AppCompatActivity {

    private LinearLayout gridContainer;
    private Button btnSelectedCount;
    private EditText etSearch;
    private final List<AppSession.ShareItem> currentItems = new ArrayList<>();
    private String currentTab = "VIDEOS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getStringExtra("START_TAB") != null) {
            currentTab = getIntent().getStringExtra("START_TAB");
        }
        requestPerms();
        setContentView(buildPickerUi());
        loadData(currentTab);
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

    private View buildPickerUi() {
        LinearLayout master = new LinearLayout(this);
        master.setOrientation(LinearLayout.VERTICAL);
        master.setBackgroundColor(Color.parseColor("#070B14"));
        master.setPadding(28, 35, 28, 25);

        etSearch = new EditText(this);
        etSearch.setHint("🔍 Search Videos, Photos, Songs, Apps, Contacts...");
        etSearch.setHintTextColor(Color.parseColor("#64748B"));
        etSearch.setTextColor(Color.WHITE);
        etSearch.setBackground(UiHelper.bentoBox("#11192E", "#263554", 30, 2));
        etSearch.setPadding(35, 25, 35, 25);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderBentoGrid(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        master.addView(etSearch);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, 20, 0, 15);

        String[] tabs = {"VIDEOS", "PHOTOS", "SONGS", "APPS", "CONTACTS"};
        for (String t : tabs) {
            Button b = new Button(this);
            b.setText(t);
            b.setTextSize(10f);
            b.setAllCaps(false);
            b.setTextColor(Color.WHITE);
            b.setBackground(UiHelper.bentoBox("#151E32", "#00F5D4", 24, 1));
            UiHelper.enableTvFocus(b);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 105, 1f);
            lp.setMargins(4, 0, 4, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                currentTab = t;
                loadData(t);
            });
            tabRow.addView(b);
        }
        master.addView(tabRow);

        Button btnAll = new Button(this);
        btnAll.setText("☑ Select All in Tab");
        btnAll.setAllCaps(false);
        btnAll.setTextColor(Color.parseColor("#00F5D4"));
        btnAll.setBackgroundColor(Color.TRANSPARENT);
        btnAll.setOnClickListener(v -> {
            for (AppSession.ShareItem item : currentItems) {
                if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
            }
            renderBentoGrid(etSearch.getText().toString());
            updateCount();
        });
        master.addView(btnAll);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(gridContainer);
        master.addView(scroll);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(0, 20, 0, 5);

        btnSelectedCount = UiHelper.makeBtn(this, "0 SELECTED", "#11192E", "#00F5D4");
        btnSelectedCount.setLayoutParams(new LinearLayout.LayoutParams(0, 135, 1f));

        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(20, 10));

        Button btnNext = UiHelper.makeBtn(this, "TURBO SEND ➔", "#00F5D4", "#070B14");
        btnNext.setLayoutParams(new LinearLayout.LayoutParams(0, 135, 1f));
        btnNext.setOnClickListener(v -> {
            if (AppSession.selectedQueue.isEmpty()) {
                Toast.makeText(this, "Select at least 1 file first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, ConnectTransferActivity.class);
            i.putExtra("MODE", "SEND");
            startActivity(i);
        });

        bottomBar.addView(btnSelectedCount);
        bottomBar.addView(sp);
        bottomBar.addView(btnNext);
        master.addView(bottomBar);

        updateCount();
        return master;
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
            } else if ("CONTACTS".equals(tab)) {
                try (Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
                    if (c != null) {
                        int count = 0;
                        File vcfDir = new File(getCacheDir(), "vcf");
                        if (!vcfDir.exists()) vcfDir.mkdirs();
                        while (c.moveToNext() && count < 60) {
                            String name = c.getString(0);
                            String num = c.getString(1);
                            if (name != null && num != null) {
                                File vcf = new File(vcfDir, name.replaceAll("[^a-zA-Z0-9]", "_") + ".vcf");
                                String vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:" + name + "\nTEL:" + num + "\nEND:VCARD";
                                try (FileOutputStream fos = new FileOutputStream(vcf)) {
                                    fos.write(vcard.getBytes());
                                }
                                currentItems.add(new AppSession.ShareItem(name + " (" + num + ").vcf", vcf.getAbsolutePath(), vcf.length(), "CONTACTS"));
                                count++;
                            }
                        }
                    }
                }
            } else {
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                if ("PHOTOS".equals(tab)) uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("SONGS".equals(tab)) uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE};
                try (Cursor c = getContentResolver().query(uri, proj, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 60) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            if (path != null && new File(path).exists() && size > 0) {
                                currentItems.add(new AppSession.ShareItem(name != null ? name : "Media_" + count, path, size, tab));
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        renderBentoGrid(etSearch.getText().toString());
    }

    private void renderBentoGrid(String query) {
        gridContainer.removeAllViews();
        String q = query.toLowerCase().trim();
        List<AppSession.ShareItem> filtered = new ArrayList<>();
        for (AppSession.ShareItem it : currentItems) {
            if (q.isEmpty() || it.name.toLowerCase().contains(q)) filtered.add(it);
        }

        for (int i = 0; i < filtered.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(-1, -2);
            rLp.bottomMargin = 18;
            row.setLayoutParams(rLp);

            row.addView(createThumbnailCard(filtered.get(i)));

            View space = new View(this);
            space.setLayoutParams(new LinearLayout.LayoutParams(18, 10));
            row.addView(space);

            if (i + 1 < filtered.size()) {
                row.addView(createThumbnailCard(filtered.get(i + 1)));
            } else {
                View dummy = new View(this);
                dummy.setLayoutParams(new LinearLayout.LayoutParams(0, 10, 1f));
                row.addView(dummy);
            }
            gridContainer.addView(row);
        }
    }

    @SuppressWarnings("deprecation")
    private View createThumbnailCard(AppSession.ShareItem item) {
        boolean isSel = AppSession.selectedQueue.contains(item);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(UiHelper.bentoBox("#11192E", isSel ? "#00F5D4" : "#1E293B", 30, isSel ? 4 : 2));
        card.setPadding(20, 22, 20, 22);
        UiHelper.enableTvFocus(card);
        card.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new LinearLayout.LayoutParams(130, 130));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

        try {
            if ("APPS".equals(item.category)) {
                PackageManager pm = getPackageManager();
                Drawable icon = pm.getPackageArchiveInfo(item.path, 0) != null
                        ? pm.getPackageArchiveInfo(item.path, 0).applicationInfo.loadIcon(pm)
                        : getApplicationInfo().loadIcon(pm);
                thumb.setImageDrawable(icon);
            } else if ("VIDEOS".equals(item.category)) {
                Bitmap bmp = ThumbnailUtils.createVideoThumbnail(item.path, MediaStore.Video.Thumbnails.MICRO_KIND);
                if (bmp != null) thumb.setImageBitmap(bmp);
                else thumb.setBackground(UiHelper.box("#A855F7", 25));
            } else if ("PHOTOS".equals(item.category)) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 8; // Memory safe for Android 5 to 16
                Bitmap bmp = BitmapFactory.decodeFile(item.path, opts);
                if (bmp != null) thumb.setImageBitmap(bmp);
                else thumb.setBackground(UiHelper.box("#38BDF8", 25));
            } else {
                thumb.setBackground(UiHelper.box("#10B981", 65));
            }
        } catch (Throwable t) {
            thumb.setBackground(UiHelper.box("#1E293B", 25));
        }

        TextView tvName = new TextView(this);
        tvName.setText(item.name);
        tvName.setMaxLines(1);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(12f);
        tvName.setPadding(0, 12, 0, 4);

        TextView tvSize = new TextView(this);
        tvSize.setText(AppSession.formatSize(item.size));
        tvSize.setTextColor(Color.parseColor("#38BDF8"));
        tvSize.setTextSize(11f);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(isSel);
        cb.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                if (!AppSession.selectedQueue.contains(item)) AppSession.selectedQueue.add(item);
            } else {
                AppSession.selectedQueue.remove(item);
            }
            card.setBackground(UiHelper.bentoBox("#11192E", checked ? "#00F5D4" : "#1E293B", 30, checked ? 4 : 2));
            updateCount();
        });

        card.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
        card.addView(thumb);
        card.addView(tvName);
        card.addView(tvSize);
        card.addView(cb);
        return card;
    }

    private void updateCount() {
        long bytes = 0;
        for (AppSession.ShareItem i : AppSession.selectedQueue) bytes += i.size;
        btnSelectedCount.setText(AppSession.selectedQueue.size() + " (" + AppSession.formatSize(bytes) + ")");
    }
}
