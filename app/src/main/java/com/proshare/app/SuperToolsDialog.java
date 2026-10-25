package com.proshare.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class SuperToolsDialog {

    // Real Built-in Video & Audio Player so user can preview any video or song right inside the app
    public static void openInAppMediaPlayer(final Activity act, final AppSession.ShareItem item) {
        if (item == null || item.path == null) return;
        String lower = item.name.toLowerCase();

        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || "VIDEOS".equals(item.category)) {
            LinearLayout box = new LinearLayout(act);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(Color.BLACK);
            box.setPadding(10, 10, 10, 10);

            final VideoView vv = new VideoView(act);
            vv.setVideoURI(Uri.fromFile(new File(item.path)));
            MediaController mc = new MediaController(act);
            mc.setAnchorView(vv);
            vv.setMediaController(mc);
            vv.setLayoutParams(new LinearLayout.LayoutParams(-1, 720));
            box.addView(vv);
            vv.start();

            new AlertDialog.Builder(act)
                    .setTitle(item.name)
                    .setView(box)
                    .setPositiveButton("Close", (d, w) -> vv.stopPlayback())
                    .show();
        } else if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || "SONGS".equals(item.category)) {
            try {
                final MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(item.path);
                mp.prepare();
                mp.start();
                new AlertDialog.Builder(act)
                        .setTitle("Playing Audio")
                        .setMessage(item.name + "\n(" + item.durationOrSub + ")")
                        .setCancelable(false)
                        .setPositiveButton("Stop", (d, w) -> {
                            try {
                                mp.stop();
                                mp.release();
                            } catch (Exception ignored) {}
                        })
                        .show();
            } catch (Exception e) {
                Toast.makeText(act, "Unable to play audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void backupSelectedApks(Activity act) {
        List<AppSession.ShareItem> apps = AppSession.getCategoryItems(act, "APPS");
        if (apps.isEmpty()) {
            Toast.makeText(act, "No apps found to backup.", Toast.LENGTH_SHORT).show();
            return;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare_APKs");
        if (!dir.exists()) dir.mkdirs();

        int saved = 0;
        for (int i = 0; i < Math.min(5, apps.size()); i++) {
            try {
                AppSession.ShareItem it = apps.get(i);
                File src = new File(it.path);
                File dst = new File(dir, it.name);
                try (FileInputStream fis = new FileInputStream(src); FileOutputStream fos = new FileOutputStream(dst)) {
                    byte[] buf = new byte[131072];
                    int r;
                    while ((r = fis.read(buf)) != -1) fos.write(buf, 0, r);
                }
                saved++;
            } catch (Exception ignored) {}
        }
        Toast.makeText(act, "Saved " + saved + " APKs to /Download/HyperShare_APKs", Toast.LENGTH_LONG).show();
    }
}
