package com.proshare.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class AppSession {

    public static class ShareItem {
        public String name;
        public String path;
        public long size;
        public String category;
        public String dateStr;

        public ShareItem(String name, String path, long size, String category) {
            this.name = name;
            this.path = path;
            this.size = size;
            this.category = category;
            this.dateStr = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US).format(new Date());
        }
    }

    public static final List<ShareItem> selectedQueue = new ArrayList<>();
    public static final List<ShareItem> historyVault = new ArrayList<>();
    public static final Map<String, List<ShareItem>> ramCache = new HashMap<>();

    // Next-Level Toggles
    public static boolean smartCompressEnabled = false;
    public static boolean autoZipEnabled = true;
    public static String safeTransferPin = ""; // Empty = Unlocked, 4-digit = PIN Locked

    private static boolean preloading = false;
    private static final String PREF = "HyperSharePrefs";

    public static void preloadAllMediaAsync(Context ctx) {
        if (preloading) return;
        preloading = true;
        final Context appCtx = ctx.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            getCategoryItems(appCtx, "APPS");
            getCategoryItems(appCtx, "VIDEOS");
            getCategoryItems(appCtx, "FILES");
            getCategoryItems(appCtx, "PHOTOS");
            getCategoryItems(appCtx, "SONGS");
            preloading = false;
        });
    }

    // 1-Click Phone Clone / Phone Switch: Queues Apps, Photos, Videos & Songs
    public static int preparePhoneCloneBatch(Context ctx) {
        selectedQueue.clear();
        selectedQueue.addAll(getCategoryItems(ctx, "APPS"));
        selectedQueue.addAll(getCategoryItems(ctx, "PHOTOS"));
        selectedQueue.addAll(getCategoryItems(ctx, "VIDEOS"));
        selectedQueue.addAll(getCategoryItems(ctx, "SONGS"));
        return selectedQueue.size();
    }

    // Smart Storage Warning Checker
    public static boolean hasEnoughFreeStorage(long neededBytes) {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            long free = s.getAvailableBlocksLong() * s.getBlockSizeLong();
            return free > (neededBytes + 52428800L); // +50MB safety margin
        } catch (Exception e) {
            return true;
        }
    }

    public static long getSelectedTotalBytes() {
        long total = 0;
        for (ShareItem item : selectedQueue) total += item.size;
        return total;
    }

    // Smart Duplicate File Finder (Cleaner Engine)
    public static List<ShareItem> findDuplicateFiles(Context ctx) {
        List<ShareItem> all = new ArrayList<>();
        all.addAll(getCategoryItems(ctx, "VIDEOS"));
        all.addAll(getCategoryItems(ctx, "PHOTOS"));
        all.addAll(getCategoryItems(ctx, "SONGS"));

        Map<String, ShareItem> seen = new HashMap<>();
        List<ShareItem> duplicates = new ArrayList<>();
        for (ShareItem it : all) {
            String key = it.name + "_" + it.size;
            if (seen.containsKey(key)) {
                duplicates.add(it);
            } else {
                seen.put(key, it);
            }
        }
        return duplicates;
    }

    public static List<ShareItem> getCategoryItems(Context ctx, String tab) {
        if ("HISTORY".equals(tab) && !historyVault.isEmpty()) {
            return new ArrayList<>(historyVault);
        }
        if (ramCache.containsKey(tab) && !ramCache.get(tab).isEmpty()) {
            return new ArrayList<>(ramCache.get(tab));
        }

        List<ShareItem> list = new ArrayList<>();
        try {
            if ("APPS".equals(tab)) {
                PackageManager pm = ctx.getPackageManager();
                for (ApplicationInfo app : pm.getInstalledApplications(0)) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && app.sourceDir != null) {
                        File f = new File(app.sourceDir);
                        list.add(new ShareItem(pm.getApplicationLabel(app).toString() + ".apk", f.getAbsolutePath(), f.length(), "APPS"));
                    }
                }
            } else {
                Uri uri = MediaStore.Files.getContentUri("external");
                if ("VIDEOS".equals(tab)) uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                else if ("PHOTOS".equals(tab)) uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("SONGS".equals(tab)) uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE};
                try (Cursor c = ctx.getContentResolver().query(uri, proj, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 120) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            if (path != null && new File(path).exists() && size > 0) {
                                list.add(new ShareItem(name != null ? name : "File_" + count, path, size, tab));
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (!list.isEmpty()) {
            ramCache.put(tab, list);
        }
        return list;
    }

    public static void addStats(Context ctx, long sentBytes, long recBytes, int files) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit()
            .putLong("sent", sp.getLong("sent", 0) + sentBytes)
            .putLong("rec", sp.getLong("rec", 0) + recBytes)
            .putInt("files", sp.getInt("files", 0) + files)
            .apply();
    }

    public static String getSentText(Context ctx) {
        return formatSize(ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("sent", 0));
    }

    public static String getRecText(Context ctx) {
        return formatSize(ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("rec", 0));
    }

    public static int getFilesCount(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("files", 0);
    }

    public static String formatSize(long b) {
        if (b >= 1073741824L) return String.format(Locale.US, "%.2f GB", b / 1073741824.0);
        if (b >= 1048576L) return String.format(Locale.US, "%.1f MB", b / 1048576.0);
        return String.format(Locale.US, "%.0f KB", Math.max(1.0, b / 1024.0));
    }

    public static String getStorageText() {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            long total = s.getBlockCountLong() * s.getBlockSizeLong();
            long used = total - (s.getAvailableBlocksLong() * s.getBlockSizeLong());
            return "Internal Storage: " + formatSize(used) + " / " + formatSize(total);
        } catch (Exception e) {
            return "Internal Storage: Available";
        }
    }
}
