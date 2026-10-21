package com.proshare.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppSession {

    public static class ShareItem {
        public String name;
        public String path;
        public long size;
        public String category;

        public ShareItem(String name, String path, long size, String category) {
            this.name = name;
            this.path = path;
            this.size = size;
            this.category = category;
        }
    }

    public static final List<ShareItem> selectedQueue = new ArrayList<>();
    private static final String PREF = "HyperSharePrefs";

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
            return "Internal Storage: Ready";
        }
    }
}
