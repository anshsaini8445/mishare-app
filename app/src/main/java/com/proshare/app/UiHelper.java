package com.proshare.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

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

    public static GradientDrawable circleGradient(String startHex, String endHex) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor(startHex), Color.parseColor(endHex)}
        );
        gd.setShape(GradientDrawable.OVAL);
        return gd;
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attachSmoothBounce(View v) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnTouchListener((view, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                HapticSoundEngine.tick(view);
                view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
            return false;
        });
    }

    public static Button makeBtn(Context ctx, String text, String bgHex, String textHex) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor(textHex));
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(box(bgHex, 60));
        attachSmoothBounce(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 130);
        lp.topMargin = 16;
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

    // 1. INSHARE-STYLE CLEAN SEND & RECEIVE CIRCLES
    public static class HeroCircleView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path iconPath = new Path();
        private final boolean isSend;

        public HeroCircleView(Context ctx, boolean isSend) {
            super(ctx);
            this.isSend = isSend;
            bgPaint.setColor(Color.parseColor(isSend ? "#16CC3E" : "#1890FF"));
            shadowPaint.setColor(Color.parseColor(isSend ? "#2616CC3E" : "#261890FF"));
            iconPaint.setColor(Color.WHITE);
            iconPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 14f;

            canvas.drawCircle(cx, cy + 8f, r + 6f, shadowPaint);
            canvas.drawCircle(cx, cy, r, bgPaint);
            iconPath.reset();

            if (isSend) {
                iconPath.moveTo(cx - r * 0.40f, cy + r * 0.05f);
                iconPath.lineTo(cx + r * 0.40f, cy - r * 0.36f);
                iconPath.lineTo(cx + r * 0.08f, cy + r * 0.40f);
                iconPath.lineTo(cx - r * 0.08f, cy + r * 0.14f);
                iconPath.close();
                canvas.drawPath(iconPath, iconPaint);
            } else {
                canvas.drawRect(cx - r * 0.13f, cy - r * 0.40f, cx + r * 0.13f, cy - r * 0.26f, iconPaint);
                canvas.drawRect(cx - r * 0.13f, cy - r * 0.18f, cx + r * 0.13f, cy + r * 0.08f, iconPaint);
                iconPath.moveTo(cx - r * 0.36f, cy + r * 0.04f);
                iconPath.lineTo(cx + r * 0.36f, cy + r * 0.04f);
                iconPath.lineTo(cx, cy + r * 0.42f);
                iconPath.close();
                canvas.drawPath(iconPath, iconPaint);
            }
        }
    }

    // 2. CLEAN CIRCULAR PROGRESS RING WITH SPEED & ETA (Shown ONLY during active transfer)
    public static class LiquidWaveSphereView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pctPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint etaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcRect = new RectF();

        private int percent = 0;
        private String etaText = "Connecting...";

        public LiquidWaveSphereView(Context ctx) {
            super(ctx);
            trackPaint.setColor(Color.parseColor("#E2E8F0"));
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(18f);

            progressPaint.setColor(Color.parseColor("#16CC3E"));
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
            progressPaint.setStrokeWidth(18f);

            pctPaint.setColor(Color.parseColor("#0F172A"));
            pctPaint.setTextSize(62f);
            pctPaint.setTypeface(Typeface.DEFAULT_BOLD);
            pctPaint.setTextAlign(Paint.Align.CENTER);

            etaPaint.setColor(Color.parseColor("#64748B"));
            etaPaint.setTextSize(28f);
            etaPaint.setTypeface(Typeface.DEFAULT_BOLD);
            etaPaint.setTextAlign(Paint.Align.CENTER);
        }

        public void updateTransferState(int pct, double mbps, long remainingBytes) {
            this.percent = Math.max(0, Math.min(100, pct));
            double bytesPerSec = (mbps * 1048576.0) / 8.0;
            if (pct >= 100) {
                this.etaText = "Transfer Complete";
            } else if (bytesPerSec > 1024 && remainingBytes > 0) {
                long sec = (long) (remainingBytes / bytesPerSec);
                long min = sec / 60;
                long remS = sec % 60;
                this.etaText = min > 0
                        ? String.format(Locale.US, "%dm %02ds remaining", min, remS)
                        : String.format(Locale.US, "%ds remaining", Math.max(1, remS));
            } else {
                this.etaText = "Transferring...";
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 24f;

            arcRect.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawCircle(cx, cy, r, trackPaint);
            canvas.drawArc(arcRect, -90f, (percent * 360f) / 100f, false, progressPaint);

            canvas.drawText(percent + "%", cx, cy - 4f, pctPaint);
            canvas.drawText(etaText, cx, cy + 42f, etaPaint);
        }
    }

    // 3. GREEN LASER QR SCANNER FRAME
    public static class QrLaserFrameView extends View {
        private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float laserY = 25f;

        public QrLaserFrameView(Context ctx) {
            super(ctx);
            cornerPaint.setColor(Color.parseColor("#16CC3E"));
            cornerPaint.setStyle(Paint.Style.STROKE);
            cornerPaint.setStrokeWidth(10f);

            laserPaint.setColor(Color.parseColor("#16CC3E"));
            laserPaint.setStrokeWidth(5f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            int len = 55;

            canvas.drawLine(12, 12, 12 + len, 12, cornerPaint);
            canvas.drawLine(12, 12, 12, 12 + len, cornerPaint);
            canvas.drawLine(w - 12 - len, 12, w - 12, 12, cornerPaint);
            canvas.drawLine(w - 12, 12, w - 12, 12 + len, cornerPaint);
            canvas.drawLine(12, h - 12 - len, 12, h - 12, cornerPaint);
            canvas.drawLine(12, h - 12, 12 + len, h - 12, cornerPaint);
            canvas.drawLine(w - 12 - len, h - 12, w - 12, h - 12, cornerPaint);
            canvas.drawLine(w - 12, h - 12 - len, w - 12, h - 12, cornerPaint);

            canvas.drawLine(30, laserY, w - 30, laserY, laserPaint);
            laserY += 6f;
            if (laserY > h - 30) laserY = 30f;
            postInvalidateDelayed(25);
        }
    }

    // 4. SMOOTH GREEN RADAR SWEEP (Like InShare Searching Bottom Sheet)
    public static class RadarSweepView extends View {
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private float angle = 0f;

        public RadarSweepView(Context context) {
            super(context);
            circlePaint.setColor(Color.parseColor("#E8F8EE"));
            circlePaint.setStyle(Paint.Style.FILL);

            arcPaint.setColor(Color.parseColor("#16CC3E"));
            arcPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 6f;
            canvas.drawCircle(cx, cy, r, circlePaint);
            oval.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(oval, angle, 95f, true, arcPaint);
            angle = (angle + 5f) % 360f;
            postInvalidateDelayed(25);
        }
    }
}
