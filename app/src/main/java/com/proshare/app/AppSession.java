package com.proshare.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
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
        public String durationOrSub;
        public String packageName;

        public ShareItem(String name, String path, long size, String category, String durationOrSub, String packageName) {
            this.name = name;
            this.path = path;
            this.size = size;
            this.category = category;
            this.durationOrSub = durationOrSub;
            this.packageName = packageName;
        }
    }

    public static final List<ShareItem> selectedQueue = new ArrayList<>();
    public static final List<ShareItem> historyVault = new ArrayList<>();
    public static final Map<String, List<ShareItem>> ramCache = new HashMap<>();
    private static boolean preloading = false;
    private static final String PREF = "HyperSharePrefs";

    public static void preloadAllMediaAsync(Context ctx) {
        if (preloading) return;
        preloading = true;
        final Context appCtx = ctx.getApplicationContext();
        Executors.newFixedThreadPool(2).execute(() -> {
            getCategoryItems(appCtx, "APPS");
            getCategoryItems(appCtx, "VIDEOS");
            getCategoryItems(appCtx, "PHOTOS");
            getCategoryItems(appCtx, "SONGS");
            getCategoryItems(appCtx, "FILES");
            preloading = false;
        });
    }

    public static List<ShareItem> getCategoryItems(Context ctx, String tab) {
        if ("HISTORY".equals(tab)) {
            return new ArrayList<>(historyVault);
        }
        if (ramCache.containsKey(tab) && !ramCache.get(tab).isEmpty()) {
            return new ArrayList<>(ramCache.get(tab));
        }

        List<ShareItem> list = new ArrayList<>();
        try {
            if ("APPS".equals(tab)) {
                PackageManager pm = ctx.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                for (ApplicationInfo app : apps) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && app.sourceDir != null) {
                        File f = new File(app.sourceDir);
                        if (f.exists()) {
                            String label = pm.getApplicationLabel(app).toString();
                            list.add(new ShareItem(label + ".apk", f.getAbsolutePath(), f.length(), "APPS", formatSize(f.length()), app.packageName));
                        }
                    }
                }
            } else if ("CONTACTS".equals(tab)) {
                try (Cursor c = ctx.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                        null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
                    if (c != null) {
                        File vcfDir = new File(ctx.getCacheDir(), "contacts_vcf");
                        if (!vcfDir.exists()) vcfDir.mkdirs();
                        int count = 0;
                        while (c.moveToNext() && count < 150) {
                            String name = c.getString(0);
                            String num = c.getString(1);
                            if (name != null && num != null) {
                                File vcf = new File(vcfDir, name.replaceAll("[^a-zA-Z0-9]", "_") + ".vcf");
                                String data = "BEGIN:VCARD\nVERSION:3.0\nFN:" + name + "\nTEL:" + num + "\nEND:VCARD";
                                try (FileOutputStream fos = new FileOutputStream(vcf)) {
                                    fos.write(data.getBytes());
                                }
                                list.add(new ShareItem(name + ".vcf", vcf.getAbsolutePath(), vcf.length(), "CONTACTS", num, ""));
                                count++;
                            }
                        }
                    }
                }
            } else if ("VIDEOS".equals(tab)) {
                String[] proj = {
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DURATION
                };
                try (Cursor c = ctx.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                        MediaStore.Video.Media.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 200) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            long durMs = c.getLong(3);
                            if (path != null && new File(path).exists() && size > 0) {
                                list.add(new ShareItem(name != null ? name : "Video_" + count + ".mp4", path, size, "VIDEOS", formatDuration(durMs), ""));
                                count++;
                            }
                        }
                    }
                }
            } else if ("PHOTOS".equals(tab)) {
                String[] proj = {
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.SIZE
                };
                try (Cursor c = ctx.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                        MediaStore.Images.Media.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 300) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            if (path != null && new File(path).exists() && size > 0) {
                                list.add(new ShareItem(name != null ? name : "IMG_" + count + ".jpg", path, size, "PHOTOS", formatSize(size), ""));
                                count++;
                            }
                        }
                    }
                }
            } else if ("SONGS".equals(tab)) {
                String[] proj = {
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DURATION
                };
                try (Cursor c = ctx.getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                        MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 250) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            String artist = c.getString(3);
                            long dur = c.getLong(4);
                            if (path != null && new File(path).exists() && size > 0) {
                                String sub = (artist != null && !artist.contains("<unknown>")) ? artist + " • " + formatDuration(dur) : formatDuration(dur);
                                list.add(new ShareItem(name != null ? name : "Audio_" + count + ".mp3", path, size, "SONGS", sub, ""));
                                count++;
                            }
                        }
                    }
                }
            } else {
                String[] proj = {
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        MediaStore.Files.FileColumns.DATA,
                        MediaStore.Files.FileColumns.SIZE
                };
                try (Cursor c = ctx.getContentResolver().query(
                        MediaStore.Files.getContentUri("external"), proj, null, null,
                        MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        int count = 0;
                        while (c.moveToNext() && count < 200) {
                            String name = c.getString(0);
                            String path = c.getString(1);
                            long size = c.getLong(2);
                            if (path != null && new File(path).exists() && size > 1024) {
                                String fName = name != null ? name : new File(path).getName();
                                String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date(new File(path).lastModified()));
                                list.add(new ShareItem(fName, path, size, "FILES", dateStr, ""));
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

    public static String formatDuration(long ms) {
        if (ms <= 0) return "00:00";
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    public static long getSelectedTotalBytes() {
        long total = 0;
        for (ShareItem item : selectedQueue) total += item.size;
        return total;
    }

    public static boolean hasEnoughFreeStorage(long neededBytes) {
        try {
            StatFs s = new StatFs(Environment.getDataDirectory().getPath());
            long free = s.getAvailableBlocksLong() * s.getBlockSizeLong();
            return free > (neededBytes + 31457280L);
        } catch (Exception e) {
            return true;
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
