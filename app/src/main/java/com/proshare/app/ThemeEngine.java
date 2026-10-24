package com.proshare.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.Calendar;

public class ThemeEngine {

    private static final String PREF = "HyperThemePref";

    public static final int THEME_CYBER_NEON = 0;
    public static final int THEME_AMOLED_BLACK = 1;
    public static final int THEME_OCEAN_DAYLIGHT = 2;
    public static final int THEME_SUNSET_GRADIENT = 3;
    public static final int THEME_DYNAMIC_GLASS = 4;
    public static final int THEME_AUTO_DAY_NIGHT = 5;

    public static int getRawTheme(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("theme_id", THEME_CYBER_NEON);
    }

    public static int getActiveTheme(Context ctx) {
        int raw = getRawTheme(ctx);
        if (raw == THEME_AUTO_DAY_NIGHT) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            return (hour >= 6 && hour < 18) ? THEME_OCEAN_DAYLIGHT : THEME_AMOLED_BLACK;
        }
        return raw;
    }

    public static void setTheme(Context ctx, int id) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("theme_id", id).apply();
    }

    public static void nextTheme(Context ctx) {
        setTheme(ctx, (getRawTheme(ctx) + 1) % 6);
    }

    public static String getThemeName(Context ctx) {
        switch (getRawTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "🌑 AMOLED Pure Pitch Black";
            case THEME_OCEAN_DAYLIGHT: return "🌊 Ocean Blue & Pure White";
            case THEME_SUNSET_GRADIENT: return "🌅 Sunset Orange/Purple Gradient";
            case THEME_DYNAMIC_GLASS: return "💎 Dynamic Material Glass";
            case THEME_AUTO_DAY_NIGHT: return "🌗 Auto Day/Night Switch";
            default: return "⚡ Cyber-Neon Green/Cyan";
        }
    }

    public static String bg(Context ctx) {
        switch (getActiveTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "#000000";
            case THEME_OCEAN_DAYLIGHT: return "#F8FAFC";
            case THEME_SUNSET_GRADIENT: return "#1E0B28";
            case THEME_DYNAMIC_GLASS: return "#071A24";
            default: return "#070B14";
        }
    }

    public static String card(Context ctx) {
        switch (getActiveTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "#0D0D0D";
            case THEME_OCEAN_DAYLIGHT: return "#FFFFFF";
            case THEME_SUNSET_GRADIENT: return "#311342";
            case THEME_DYNAMIC_GLASS: return "#0E2F3F";
            default: return "#11192E";
        }
    }

    public static String accent(Context ctx) {
        switch (getActiveTheme(ctx)) {
            case THEME_AMOLED_BLACK: return "#00E676";
            case THEME_OCEAN_DAYLIGHT: return "#0284C7";
            case THEME_SUNSET_GRADIENT: return "#FF6B6B";
            case THEME_DYNAMIC_GLASS: return "#38BDF8";
            default: return "#00F5D4";
        }
    }

    public static String textMain(Context ctx) {
        return getActiveTheme(ctx) == THEME_OCEAN_DAYLIGHT ? "#0F172A" : "#FFFFFF";
    }

    public static String textSub(Context ctx) {
        return getActiveTheme(ctx) == THEME_OCEAN_DAYLIGHT ? "#64748B" : "#94A3B8";
    }

    // GLOWING ONE-HAND MODE (Single-Hand Reachability)
    public static boolean isOneHandMode(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("one_hand", false);
    }

    public static void toggleOneHandMode(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putBoolean("one_hand", !sp.getBoolean("one_hand", false)).apply();
    }

    public static View applyOneHandWrapper(final Activity act, View content) {
        if (!isOneHandMode(act)) return content;

        LinearLayout outer = new LinearLayout(act);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(Color.parseColor("#020617"));
        outer.setGravity(Gravity.BOTTOM);

        // Glowing Top Banner in One-Hand Mode to exit anytime
        Button exitBanner = new Button(act);
        exitBanner.setText("🖐️ ONE-HAND MODE ACTIVE  •  TAP TO FULLSCREEN ⬆");
        exitBanner.setAllCaps(false);
        exitBanner.setTextColor(Color.parseColor("#070B14"));
        exitBanner.setTypeface(null, Typeface.BOLD);
        exitBanner.setBackground(UiHelper.gradientBento("#FACC15", "#00F5D4", 35));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-1, 110);
        bLp.setMargins(35, 30, 35, 15);
        exitBanner.setLayoutParams(bLp);
        exitBanner.setOnClickListener(v -> {
            HapticSoundEngine.tick(v);
            toggleOneHandMode(act);
            act.recreate();
        });
        outer.addView(exitBanner);

        int screenH = act.getResources().getDisplayMetrics().heightPixels;
        int compactH = (int) (screenH * 0.66f);
        FrameLayout holder = new FrameLayout(act);
        holder.setLayoutParams(new LinearLayout.LayoutParams(-1, compactH));
        holder.addView(content);
        outer.addView(holder);
        return outer;
    }
}
