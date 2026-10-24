package com.proshare.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuperToolsDialog {

    public static void showHub(Context ctx, MultiTransferEngine engine) {
        String[] tools = {
                "📲 Phone Clone / Switch (1-Click Old to New Phone)",
                "🔐 Safe Share Vault (Set 4-Digit PIN Lock)",
                "🧹 Smart Duplicate File Cleaner (Free Up Space)",
                "📦 APK Extractor & Local Backup",
                "🚀 Built-in 5GHz Speed Booster Test",
                "💬 Offline Chat & Clipboard Text Share",
                "🗜️ Auto-Zip & Smart Compress (" + (AppSession.autoZipEnabled ? "ON" : "OFF") + ")",
                "🎬 In-App Media Player (Play Received Media)"
        };

        new AlertDialog.Builder(ctx)
                .setTitle("🛠️ HyperShare Super Tools Hub")
                .setItems(tools, (d, which) -> {
                    if (which == 0) runPhoneClone(ctx);
                    else if (which == 1) showPinVaultDialog(ctx);
                    else if (which == 2) runSmartDuplicateCleaner(ctx);
                    else if (which == 3) backupSelectedApks(ctx);
                    else if (which == 4) runSpeedBoosterBenchmark(ctx);
                    else if (which == 5) showOfflineChatDialog(ctx, engine);
                    else if (which == 6) {
                        AppSession.autoZipEnabled = !AppSession.autoZipEnabled;
                        AppSession.smartCompressEnabled = AppSession.autoZipEnabled;
                        Toast.makeText(ctx, "Auto-Zip & Smart Compress: " + (AppSession.autoZipEnabled ? "ENABLED ⚡" : "OFF"), Toast.LENGTH_SHORT).show();
                    } else if (which == 7) showInAppMediaPlayerPicker(ctx);
                })
                .setPositiveButton("Close", null)
                .show();
    }

    // 1. PHONE CLONE (Selects Apps, Videos, Photos, Songs in 1-Click)
    private static void runPhoneClone(Context ctx) {
        AppSession.selectedQueue.clear();
        AppSession.selectedQueue.addAll(AppSession.getCategoryItems(ctx, "APPS"));
        AppSession.selectedQueue.addAll(AppSession.getCategoryItems(ctx, "VIDEOS"));
        AppSession.selectedQueue.addAll(AppSession.getCategoryItems(ctx, "PHOTOS"));
        AppSession.selectedQueue.addAll(AppSession.getCategoryItems(ctx, "SONGS"));

        HapticSoundEngine.popSuccess(ctx);
        Intent i = new Intent(ctx, ConnectTransferActivity.class);
        i.putExtra("MODE", "SEND");
        ctx.startActivity(i);
    }

    // 2. SAFE SHARE (4-Digit PIN Protection)
    private static void showPinVaultDialog(Context ctx) {
        EditText et = new EditText(ctx);
        et.setHint("Enter 4-Digit Security PIN (e.g. 2580)");
        new AlertDialog.Builder(ctx)
                .setTitle("🔐 Safe Share PIN Lock")
                .setMessage("Receiver will need this 4-Digit PIN to unlock transferred private files.")
                .setView(et)
                .setPositiveButton("Set PIN", (d, w) -> {
                    AppSession.safeTransferPin = et.getText().toString().
