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
            this.dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date());
        }
    }

    public static final List<ShareItem> selectedQueue = new ArrayList<>();
    public static final Map<String, List<ShareItem>> ramCache = new HashMap<>();
    private static boolean preloading = false;
    private static final String PREF = "HyperSharePrefs";

    // Advanced Feature Flags
    public static boolean autoZipEnabled = false;
    public static boolean smartCompressEnabled = false;
    public static String safeTransferPin = ""; // Empty means unlocked, 4-digit means PIN locked

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

    public static List<ShareItem> getCategoryItems(Context ctx, String tab) {
        if ("HISTORY".equals(tab)) {
            return getHistoryVault(ctx);
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

    // 60-DAY HISTORY VAULT FOR DIRECT RESEND & IN-APP PLAY
    public static void recordHistoryItem(Context ctx, String name, String path, long size, String tag) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String old = sp.getString("history_vault", "");
        String entry = name.replace("|", "_") + "|" + path + "|" + size + "|" + tag + "\n";
        String updated = entry + old;
        String[] lines = updated.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 60); i++) {
            if (!lines[i].trim().isEmpty()) sb.append(lines[i]).append("\n");
        }
        sp.edit().putString("history_vault", sb.toString()).apply();
    }

    public static List<ShareItem> getHistoryVault(Context ctx) {
        List<ShareItem> list = new ArrayList<>();
        String raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("history_vault", "");
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|");
            if (p.length >= 4) {
                try {
                    list.add(new ShareItem(p[0], p[1], Long.parseLong(p[2]), p[3]));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    // SMART STORAGE WARNING CHECKER
    public static boolean hasEnoughFreeSpace(long incomingBytes) {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            long freeBytes = s.getAvailableBlocksLong() * s.getBlockSizeLong();
            return freeBytes > (incomingBytes + 52428800L); // Keep 50MB safety margin
        } catch (Exception e) {
            return true;
        }
    }

    public static long getFreeSpaceBytes() {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            return s.getAvailableBlocksLong() * s.getBlockSizeLong();
        } catch (Exception e) {
            return 1073741824L;
        }
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
