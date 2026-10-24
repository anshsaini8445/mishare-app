package com.proshare.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class ThemeEngine {

    private static final String PREF = "HyperThemePref";

    public static final int THEME_CYBER_NEON = 0;
    public static final int THEME_AMOLED_BLACK = 1;
    public static final int THEME_OCEAN_WHITE = 2;
    public static final int THEME_SUNSET_GRADIENT = 3;
    public static final int THEME_MATERIAL_AUTO = 4;

    public static int getTheme(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("theme_id", THEME_CYBER_NEON);
    }

    public static void setTheme(Context ctx, int id) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("theme_id", id).apply();
    }

    public static void nextTheme(Context ctx) {
        setTheme(ctx, (getTheme(ctx) + 1) % 5);
    }

    public static String getThemeName(Context ctx) {
        switch (getTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "🌑 AMOLED Pitch Black";
            case THEME_OCEAN_WHITE: return "🌊 Ocean Pure White";
            case THEME_SUNSET_GRADIENT: return "🌅 Sunset Aesthetic";
            case THEME_MATERIAL_AUTO: return "🎨 Dynamic Material / Auto Day-Night";
            default: return "⚡ Cyber-Neon Glass";
        }
    }

    private static boolean isSystemNight(Context ctx) {
        int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isLightTheme(Context ctx) {
        int t = getTheme(ctx);
        if (t == THEME_OCEAN_WHITE) return true;
        if (t == THEME_MATERIAL_AUTO) return !isSystemNight(ctx);
        return false;
    }

    public static String bg(Context ctx) {
        switch (getTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "#000000";
            case THEME_OCEAN_WHITE: return "#F8FAFC";
            case THEME_SUNSET_GRADIENT: return "#190A28";
            case THEME_MATERIAL_AUTO: return isSystemNight(ctx) ? "#0B132B" : "#F1F5F9";
            default: return "#070B14";
        }
    }

    public static String card(Context ctx) {
        switch (getTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "#111111";
            case THEME_OCEAN_WHITE: return "#FFFFFF";
            case THEME_SUNSET_GRADIENT: return "#2D144B";
            case THEME_MATERIAL_AUTO: return isSystemNight(ctx) ? "#1C2541" : "#FFFFFF";
            default: return "#11192E";
        }
    }

    public static String accent(Context ctx) {
        if (getTheme(ctx) == THEME_MATERIAL_AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                int c = ctx.getColor(android.R.color.system_accent1_400);
                return String.format("#%06X", (0xFFFFFF & c));
            } catch (Exception ignored) {}
        }
        switch (getTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "#00E676";
            case THEME_OCEAN_WHITE: return "#0284C7";
            case THEME_SUNSET_GRADIENT: return "#FF6B6B";
            case THEME_MATERIAL_AUTO: return "#3B82F6";
            default: return "#00F5D4";
        }
    }

    public static String textMain(Context ctx) {
        return isLightTheme(ctx) ? "#0F172A" : "#FFFFFF";
    }

    public static String textSub(Context ctx) {
        return isLightTheme(ctx) ? "#64748B" : "#94A3B8";
    }

    // GLOWING ONE-HAND MODE ENGINE
    public static boolean isOneHandMode(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("one_hand", false);
    }

    public static void toggleOneHandMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putBoolean("one_hand", !sp.getBoolean("one_hand", false)).apply();
    }

    public static View applyOneHandWrapper(Activity act, View content, Runnable onRefresh) {
        if (!isOneHandMode(act)) return content;

        FrameLayout outer = new FrameLayout(act);
        outer.setBackgroundColor(Color.parseColor(bg(act)));

        Button exitBar = new Button(act);
        exitBar.setText("🖐️ One-Hand Mode Active — Tap to Expand Full Screen");
        exitBar.setAllCaps(false);
        exitBar.setTextColor(Color.parseColor("#070B14"));
        exitBar.setBackground(UiHelper.box("#FACC15", 40));
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(-1, 110);
        topLp.setMargins(35, 60, 35, 20);
        topLp.gravity = Gravity.TOP;
        exitBar.setLayoutParams(topLp);
        exitBar.setOnClickListener(v -> {
            toggleOneHandMode(act);
            if (onRefresh != null) onRefresh.run();
        });

        int screenH = act.getResources().getDisplayMetrics().heightPixels;
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(-1, (int) (screenH * 0.68f));
        bottomLp.gravity = Gravity.BOTTOM;

        LinearLayout holder = new LinearLayout(act);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setBackground(UiHelper.bentoBox(bg(act), accent(act), 45, 4));
        holder.setLayoutParams(bottomLp);
        holder.addView(content, new LinearLayout.LayoutParams(-1, -1));

        outer.addView(exitBar);
        outer.addView(holder);
        return outer;
    }
}
