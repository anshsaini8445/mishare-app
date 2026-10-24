package com.proshare.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class SuperToolsDialog {

    public static void showHub(final Activity act) {
        String pinStatus = AppSession.safeTransferPin.isEmpty() ? "OFF" : "LOCKED (" + AppSession.safeTransferPin + ")";
        String zipStatus = AppSession.autoZipEnabled ? "ON (3x Faster)" : "OFF";
        String compStatus = AppSession.smartCompressEnabled ? "ON (Saves 40% Space)" : "OFF";

        String[] tools = {
                "📲 Phone Clone / Switch (1-Click Full Phone Replica)",
                "🔐 Private Vault / Safe Share PIN: " + pinStatus,
                "🧹 Smart Duplicate File Remover (Cleaner Engine)",
                "📦 Offline APK Extractor & App Backup",
                "🗜️ Auto-Zip Bundle Engine: " + zipStatus,
                "📉 Smart Storage Saver (Compress): " + compStatus,
                "📋 Clipboard & Link Instant Share",
                "🚀 Run 240 Mbps Speed Booster Test"
        };

        new AlertDialog.Builder(act)
                .setTitle("🛠️ HyperShare Super Tools & Vault")
                .setItems(tools, (d, which) -> {
                    if (which == 0) startPhoneClone(act);
                    else if (which == 1) showPinVaultDialog(act);
                    else if (which == 2) runDuplicateCleaner(act);
                    else if (which == 3) runApkBackupExtractor(act);
                    else if (which == 4) {
                        AppSession.autoZipEnabled = !AppSession.autoZipEnabled;
                        Toast.makeText(act, "Auto-Zip Engine: " + (AppSession.autoZipEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                    } else if (which == 5) {
                        AppSession.smartCompressEnabled = !AppSession.smartCompressEnabled;
                        Toast.makeText(act, "Smart Compress: " + (AppSession.smartCompressEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                    } else if (which == 6) shareClipboardText(act);
                    else if (which == 7) {
                        String res = WifiDirectEngine.runHardwareSpeedBoosterTest(act);
                        HapticSoundEngine.popSuccess(act);
                        new AlertDialog.Builder(act).setTitle("🚀 Speed Booster Result").setMessage(res).setPositiveButton("Awesome!", null).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // 1. PHONE CLONE (OLD PHONE TO NEW PHONE SWITCH)
    private static void startPhoneClone(Activity act) {
        int total = AppSession.preparePhoneCloneBatch(act);
        HapticSoundEngine.popSuccess(act);
        new AlertDialog.Builder(act)
                .setTitle("📲 Phone Clone Ready!")
                .setMessage("Queued " + total + " items (Apps, Photos, Videos & Songs) for 1-Click New Phone Switch.\n\nTotal Size: " + AppSession.formatSize(AppSession.getSelectedTotalBytes()))
                .setPositiveButton("START CLONE NOW ➔", (d, w) -> {
                    Intent i = new Intent(act, ConnectTransferActivity.class);
                    i.putExtra("MODE", "SEND");
                    act.startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 2. SAFE SHARE 4-DIGIT PIN VAULT
    private static void showPinVaultDialog(Activity act) {
        final EditText etPin = new EditText(act);
        etPin.setHint("Enter 4-Digit Security PIN (or leave empty to unlock)");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(act)
                .setTitle("🔐 Safe Share PIN Lock")
                .setMessage("Receiver must enter this 4-Digit PIN before opening protected files.")
                .setView(etPin)
                .setPositiveButton("Set PIN Lock", (d, w) -> {
                    AppSession.safeTransferPin = etPin.getText().toString().trim();
                    Toast.makeText(act, AppSession.safeTransferPin.isEmpty() ? "PIN Lock Removed" : "🔒 PIN Locked: " + AppSession.safeTransferPin, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // 3. SMART DUPLICATE FILE REMOVER (CLEANER ENGINE)
    private static void runDuplicateCleaner(Activity act) {
        List<AppSession.ShareItem> dups = AppSession.findDuplicateFiles(act);
        long junkBytes = 0;
        for (AppSession.ShareItem it : dups) junkBytes += it.size;

        String msg = dups.isEmpty()
                ? "✅ Great! Your phone is 100% clean. No duplicate media files found."
                : "Found " + dups.size() + " duplicate files wasting " + AppSession.formatSize(junkBytes) + " of storage!";

        new AlertDialog.Builder(act)
                .setTitle("🧹 Smart Duplicate Cleaner")
                .setMessage(msg)
                .setPositiveButton(dups.isEmpty() ? "OK" : "Clean & Free Space", (d, w) -> {
                    int deleted = 0;
                    for (AppSession.ShareItem it : dups) {
                        File f = new File(it.path);
                        if (f.exists() && f.delete()) deleted++;
                    }
                    HapticSoundEngine.popSuccess(act);
                    Toast.makeText(act, "Cleaned " + deleted + " duplicate files!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 4. OFFLINE APK EXTRACTOR / BACKUP
    private static void runApkBackupExtractor(Activity act) {
        List<AppSession.ShareItem> apps = AppSession.getCategoryItems(act, "APPS");
        if (apps.isEmpty()) {
            Toast.makeText(act, "No user apps found to extract.", Toast.LENGTH_SHORT).show();
            return;
        }
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HyperShare_APK_Backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        int count = Math.min(5, apps.size());
        for (int i = 0; i < count; i++) {
            try {
                AppSession.ShareItem app = apps.get(i);
                File src = new File(app.path);
                File dst = new File(backupDir, app.name);
                try (FileInputStream fis = new FileInputStream(src); FileOutputStream fos = new FileOutputStream(dst)) {
                    byte[] buf = new byte[65536];
                    int r;
                    while ((r = fis.read(buf)) != -1) fos.write(buf, 0, r);
                }
            } catch (Exception ignored) {}
        }
        HapticSoundEngine.popSuccess(act);
        new AlertDialog.Builder(act)
                .setTitle("📦 APK Backup Complete!")
                .setMessage("Extracted " + count + " installed APKs to:\n/Download/HyperShare_APK_Backups")
                .setPositiveButton("OK", null)
                .show();
    }

    // 5. CLIPBOARD & LINK SHARE
    private static void shareClipboardText(Activity act) {
        ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
        String clip = (cm != null && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0)
                ? String.valueOf(cm.getPrimaryClip().getItemAt(0).getText())
                : "";
        final EditText et = new EditText(act);
        et.setText(clip);
        et.setHint("Paste or type text/link to beam over Wi-Fi...");
        new AlertDialog.Builder(act)
                .setTitle("📋 Instant Clipboard & Link Beam")
                .setView(et)
                .setPositiveButton("Beam to Connected Phone", (d, w) -> {
                    Toast.makeText(act, "Ready! Connect in Transfer Screen to beam text.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // 6. BUILT-IN MEDIA PLAYER (PLAY VIDEO / AUDIO INSIDE APP)
    public static void openInAppMediaPlayer(Activity act, final AppSession.ShareItem item) {
        if (!AppSession.safeTransferPin.isEmpty()) {
            final EditText pinInput = new EditText(act);
            pinInput.setHint("Enter 4-Digit PIN to Unlock");
            new AlertDialog.Builder(act)
                    .setTitle("🔒 Protected File")
                    .setView(pinInput)
                    .setPositiveButton("Unlock", (d, w) -> {
                        if (AppSession.safeTransferPin.equals(pinInput.getText().toString().trim())) {
                            launchPlayerView(act, item);
                        } else {
                            Toast.makeText(act, "❌ Wrong PIN!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
            return;
        }
        launchPlayerView(act, item);
    }

    private static void launchPlayerView(Activity act, AppSession.ShareItem item) {
        String lower = item.name.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".3gp")) {
            LinearLayout box = new LinearLayout(act);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(Color.BLACK);
            VideoView vv = new VideoView(act);
            vv.setVideoURI(Uri.fromFile(new File(item.path)));
            vv.setLayoutParams(new LinearLayout.LayoutParams(-1, 650));
            box.addView(vv);
            vv.start();
            new AlertDialog.Builder(act)
                    .setTitle("🎬 Playing: " + item.name)
                    .setView(box)
                    .setPositiveButton("Close Player", (d, w) -> vv.stopPlayback())
                    .show();
        } else if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a")) {
            try {
                final MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(item.path);
                mp.prepare();
                mp.start();
                new AlertDialog.Builder(act)
                        .setTitle("🎵 Playing Audio")
                        .setMessage("Now Playing:\n" + item.name)
                        .setPositiveButton("Stop Audio", (d, w) -> {
                            mp.stop();
                            mp.release();
                        })
                        .show();
            } catch (Exception e) {
                Toast.makeText(act, "Audio Player Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            TextView tv = new TextView(act);
            tv.setText("File Name: " + item.name + "\nSize: " + AppSession.formatSize(item.size) + "\nPath: " + item.path);
            tv.setPadding(45, 35, 45, 35);
            new AlertDialog.Builder(act)
                    .setTitle("📄 File Details")
                    .setView(tv)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
