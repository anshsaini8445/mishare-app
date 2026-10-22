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
import java.util.List;
import java.util.Locale;
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
            this.dateStr = new SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(new Date());
        }
    }

    public static final List<ShareItem> selectedQueue = new ArrayList<>();
    public static final List<ShareItem> cachedApps = new ArrayList<>();
    public static final List<ShareItem> cachedVideos = new ArrayList<>();
    public static final List<ShareItem> cachedPhotos = new ArrayList<>();
    public static final List<ShareItem> cachedSongs = new ArrayList<>();
    public static final List<ShareItem> cachedFiles = new ArrayList<>();
    public static final List<ShareItem> historyList = new ArrayList<>();

    private static volatile boolean isPreloaded = false;
    private static final String PREF = "HyperSharePrefs";

    // Zero-Lag Background Pre-Loader so Send Screen opens in 0ms without spinning
    public static void preloadAllMediaAsync(Context ctx) {
        if (isPreloaded) return;
        final Context appCtx = ctx.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                loadApps(appCtx);
                loadMedia(appCtx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "VIDEOS", cachedVideos);
                loadMedia(appCtx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "PHOTOS", cachedPhotos);
                loadMedia(appCtx, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "SONGS", cachedSongs);
                loadMedia(appCtx, MediaStore.Files.getContentUri("external"), "FILES", cachedFiles);
                isPreloaded = true;
            } catch (Exception ignored) {}
        });
    }

    public static void forceRefresh(Context ctx) {
        isPreloaded = false;
        preloadAllMediaAsync(ctx);
    }

    private static void loadApps(Context ctx) {
        cachedApps.clear();
        try {
            PackageManager pm = ctx.getPackageManager();
            for (ApplicationInfo app : pm.getInstalledApplications(0)) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && app.sourceDir != null) {
                    File f = new File(app.sourceDir);
                    cachedApps.add(new ShareItem(pm.getApplicationLabel(app).toString() + ".apk", f.getAbsolutePath(), f.length(), "APPS"));
                }
            }
        } catch (Exception ignored) {}
    }

    public static void loadMedia(Context ctx, Uri uri, String cat, List<ShareItem> target) {
        target.clear();
        String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE};
        try (Cursor c = ctx.getContentResolver().query(uri, proj, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (c != null) {
                int count = 0;
                while (c.moveToNext() && count < 80) {
                    String name = c.getString(0);
                    String path = c.getString(1);
                    long size = c.getLong(2);
                    if (path != null && new File(path).exists() && size > 0) {
                        ShareItem item = new ShareItem(name != null ? name : "File_" + count, path, size, cat);
                        target.add(item);
                        if (historyList.size() < 15) historyList.add(item);
                        count++;
                    }
                }
            }
        } catch (Exception ignored) {}
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
