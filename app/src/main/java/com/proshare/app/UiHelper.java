package com.proshare.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UiHelper {

    public static GradientDrawable box(String colorHex, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(radius);
        return gd;
    }

    public static GradientDrawable bentoBox(String bgHex, String borderHex, int radius, int strokeWidth) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgHex));
        gd.setCornerRadius(radius);
        gd.setStroke(strokeWidth, Color.parseColor(borderHex));
        return gd;
    }

    public static GradientDrawable gradientBento(String startHex, String endHex, int radius) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor(startHex), Color.parseColor(endHex)}
        );
        gd.setCornerRadius(radius);
        return gd;
    }

    public static void enableTvFocus(View v) {
        v.setFocusable(true);
        v.setClickable(true);
    }

    public static Button makeBtn(Context ctx, String text, String bgHex, String textHex) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor(textHex));
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(bentoBox(bgHex, "#263554", 38, 2));
        enableTvFocus(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 135);
        lp.topMargin = 18;
        b.setLayoutParams(lp);
        return b;
    }

    public static TextView makeTitle(Context ctx, String text, String colorHex, float size) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(size);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    // 1. LIVE ANIMATED SPEED WAVE GRAPH VIEW
    public static class LiveSpeedGraphView extends View {
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path wavePath = new Path();
        private float phase = 0f;
        private float currentSpeed = 12f;

        public LiveSpeedGraphView(Context context) {
            super(context);
            linePaint.setColor(Color.parseColor("#00F5D4"));
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(7f);

            fillPaint.setColor(Color.parseColor("#3300F5D4"));
            fillPaint.setStyle(Paint.Style.FILL);
        }

        public void setSpeed(double mbps) {
            this.currentSpeed = (float) Math.max(8.0, Math.min(300.0, mbps));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            wavePath.reset();
            float midY = h * 0.6f;
            float amp = Math.min(h * 0.35f, (currentSpeed / 200f) * (h * 0.35f) + 12f);

            wavePath.moveTo(0, h);
            for (int x = 0; x <= w; x += 15) {
                float y = midY + (float) Math.sin((x * 0.025f) + phase) * amp
                               + (float) Math.cos((x * 0.012f) - phase) * (amp * 0.4f);
                wavePath.lineTo(x, y);
            }
            wavePath.lineTo(w, h);
            wavePath.close();

            canvas.drawPath(wavePath, fillPaint);
            canvas.drawPath(wavePath, linePaint);

            phase += 0.18f;
            postInvalidateDelayed(35);
        }
    }

    // 2. LIVE ROTATING AUTO-RADAR SWEEP VIEW
    public static class RadarSweepView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float angle = 0f;

        public RadarSweepView(Context context) {
            super(context);
            ringPaint.setColor(Color.parseColor("#1E3A8A"));
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(3f);

            sweepPaint.setColor(Color.parseColor("#00F5D4"));
            sweepPaint.setStrokeWidth(6f);

            dotPaint.setColor(Color.parseColor("#00F5D4"));
            dotPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float maxR = Math.min(cx, cy) - 16f;

            canvas.drawCircle(cx, cy, maxR, ringPaint);
            canvas.drawCircle(cx, cy, maxR * 0.66f, ringPaint);
            canvas.drawCircle(cx, cy, maxR * 0.33f, ringPaint);
            canvas.drawCircle(cx, cy, 12f, dotPaint);

            double rad = Math.toRadians(angle);
            float ex = cx + (float) Math.cos(rad) * maxR;
            float ey = cy + (float) Math.sin(rad) * maxR;
            canvas.drawLine(cx, cy, ex, ey, sweepPaint);

            angle = (angle + 4.5f) % 360f;
            postInvalidateDelayed(30);
        }
    }
}
