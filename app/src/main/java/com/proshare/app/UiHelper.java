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

    // Glassmorphism Frosted Card Effect
    public static GradientDrawable glassBox(Context ctx, int radius) {
        boolean light = ThemeEngine.isLightTheme(ctx);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(light ? "#E6FFFFFF" : "#D911192E"));
        gd.setCornerRadius(radius);
        gd.setStroke(2, Color.parseColor(ThemeEngine.accent(ctx)));
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
                view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(85).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }

    public static void enableTvFocus(View v) {
        attachSmoothBounce(v);
    }

    public static Button makeBtn(Context ctx, String text, String bgHex, String textHex) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor(textHex));
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(box(bgHex, 55));
        attachSmoothBounce(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 132);
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

    // 3D LIQUID WAVE SPHERE PROGRESS BAR WITH EXACT ETA TIMER & PERCENTAGE
    public static class LiquidWaveSphereView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint wave1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint wave2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private final Path wavePath1 = new Path();
        private final Path wavePath2 = new Path();

        private int percent = 0;
        private double speedMbps = 0.0;
        private String etaText = "ETA: Ready";
        private float phase = 0f;

        public LiquidWaveSphereView(Context ctx) {
            super(ctx);
            ringPaint.setColor(Color.parseColor("#00F5D4"));
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(8f);

            wave1Paint.setColor(Color.parseColor("#6600F5D4"));
            wave1Paint.setStyle(Paint.Style.FILL);

            wave2Paint.setColor(Color.parseColor("#B310B981"));
            wave2Paint.setStyle(Paint.Style.FILL);

            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(54f);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextAlign(Paint.Align.CENTER);

            subPaint.setColor(Color.parseColor("#FACC15"));
            subPaint.setTextSize(28f);
            subPaint.setTypeface(Typeface.DEFAULT_BOLD);
            subPaint.setTextAlign(Paint.Align.CENTER);
        }

        public void updateLiquidProgress(int pct, double mbps, long remainingBytes) {
            this.percent = Math.max(0, Math.min(100, pct));
            this.speedMbps = mbps;
            double bytesPerSec = (mbps * 1048576.0) / 8.0;
            if (pct >= 100) {
                this.etaText = "✅ Completed!";
            } else if (bytesPerSec > 1024 && remainingBytes > 0) {
                long sec = Math.max(1, (long) (remainingBytes / bytesPerSec));
                long min = sec / 60;
                long remSec = sec % 60;
                this.etaText = String.format(Locale.US, "⏱️ %02dm %02ds left", min, remSec);
            } else {
                this.etaText = "⚡ Turbo Streaming...";
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(cx, cy) - 12f;

            canvas.save();
            clipPath.reset();
            clipPath.addCircle(cx, cy, r, Path.Direction.CW);
            canvas.clipPath(clipPath);

            canvas.drawColor(Color.parseColor("#0B132B"));

            float waterLevel = (cy + r) - ((percent / 100f) * (2f * r));
            float amp = Math.max(10f, Math.min(28f, (float) (speedMbps / 8.0) + 10f));

            wavePath1.reset();
            wavePath2.reset();
            wavePath1.moveTo(cx - r, cy + r);
            wavePath2.moveTo(cx - r, cy + r);

            for (float x = cx - r; x <= cx + r; x += 12) {
                float y1 = waterLevel + (float) Math.sin(((x - (cx - r)) * 0.03f) + phase) * amp;
                float y2 = waterLevel + (float) Math.cos(((x - (cx - r)) * 0.03f) - phase) * amp;
                wavePath1.lineTo(x, y1);
                wavePath2.lineTo(x, y2);
            }
            wavePath1.lineTo(cx + r, cy + r);
            wavePath1.close();
            wavePath2.lineTo(cx + r, cy + r);
            wavePath2.close();

            canvas.drawPath(wavePath1, wave1Paint);
            canvas.drawPath(wavePath2, wave2Paint);

            canvas.drawText(percent + "%", cx, cy - 10f, textPaint);
            canvas.drawText(String.format(Locale.US, "⚡ %.1f Mbps", speedMbps), cx, cy + 32f, subPaint);
            canvas.drawText(etaText, cx, cy + 72f, subPaint);

            canvas.restore();
            canvas.drawCircle(cx, cy, r, ringPaint);

            phase += 0.20f;
            postInvalidateDelayed(35);
        }
    }

    // EMPTY STATE ANIMATED 3D ROBOT / RADAR ILLUSTRATION
    public static class EmptyRobotView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float floatY = 0f;
        private boolean up = true;

        public EmptyRobotView(Context ctx) { super(ctx); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = (getHeight() / 2f) + floatY;

            p.setColor(Color.parseColor("#1E293B"));
            canvas.drawRoundRect(new RectF(cx - 70, cy - 55, cx + 70, cy + 45), 28, 28, p);
            p.setColor(Color.parseColor("#00F5D4"));
            canvas.drawCircle(cx - 28, cy - 12, 12, p);
            canvas.drawCircle(cx + 28, cy - 12, 12, p);
            canvas.drawLine(cx, cy - 55, cx, cy - 85, p);
            canvas.drawCircle(cx, cy - 90, 10, p);

            floatY += up ? -0.8f : 0.8f;
            if (floatY < -12) up = false;
            if (floatY > 12) up = true;
            postInvalidateDelayed(40);
        }
    }

    public static class HeroCircleView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path iconPath = new Path();
        private final boolean isSend;

        public HeroCircleView(Context ctx, boolean isSend) {
            super(ctx);
            this.isSend = isSend;
            bgPaint.setColor(Color.parseColor(isSend ? "#16CC3E" : "#1890FF"));
            iconPaint.setColor(Color.WHITE);
            iconPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 6f;
            canvas.drawCircle(cx, cy, r, bgPaint);
            iconPath.reset();
            if (isSend) {
                iconPath.moveTo(cx - r * 0.42f, cy + r * 0.05f);
                iconPath.lineTo(cx + r * 0.42f, cy - r * 0.38f);
                iconPath.lineTo(cx + r * 0.08f, cy + r * 0.42f);
                iconPath.lineTo(cx - r * 0.08f, cy + r * 0.14f);
                iconPath.close();
                canvas.drawPath(iconPath, iconPaint);
            } else {
                canvas.drawRect(cx - r * 0.14f, cy - r * 0.42f, cx + r * 0.14f, cy - r * 0.28f, iconPaint);
                canvas.drawRect(cx - r * 0.14f, cy - r * 0.20f, cx + r * 0.14f, cy + r * 0.08f, iconPaint);
                iconPath.moveTo(cx - r * 0.38f, cy + r * 0.04f);
                iconPath.lineTo(cx + r * 0.38f, cy + r * 0.04f);
                iconPath.lineTo(cx, cy + r * 0.44f);
                iconPath.close();
                canvas.drawPath(iconPath, iconPaint);
            }
        }
    }

    public static class QrLaserFrameView extends View {
        private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float laserY = 20f;

        public QrLaserFrameView(Context ctx) {
            super(ctx);
            cornerPaint.setColor(Color.parseColor("#00E676"));
            cornerPaint.setStyle(Paint.Style.STROKE);
            cornerPaint.setStrokeWidth(10f);
            laserPaint.setColor(Color.parseColor("#00E676"));
            laserPaint.setStrokeWidth(6f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight(), len = 55;
            canvas.drawLine(10, 10, 10 + len, 10, cornerPaint);
            canvas.drawLine(10, 10, 10, 10 + len, cornerPaint);
            canvas.drawLine(w - 10 - len, 10, w - 10, 10, cornerPaint);
            canvas.drawLine(w - 10, 10, w - 10, 10 + len, cornerPaint);
            canvas.drawLine(10, h - 10 - len, 10, h - 10, cornerPaint);
            canvas.drawLine(10, h - 10, 10 + len, h - 10, cornerPaint);
            canvas.drawLine(w - 10 - len, h - 10, w - 10, h - 10, cornerPaint);
            canvas.drawLine(w - 10, h - 10 - len, w - 10, h - 10, cornerPaint);
            canvas.drawLine(25, laserY, w - 25, laserY, laserPaint);
            laserY += 7f;
            if (laserY > h - 25) laserY = 25f;
            postInvalidateDelayed(25);
        }
    }

    public static class LiveSpeedGraphView extends View {
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path wavePath = new Path();
        private float phase = 0f, currentSpeed = 10f;

        public LiveSpeedGraphView(Context context) {
            super(context);
            linePaint.setColor(Color.parseColor("#10B981"));
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(6f);
            fillPaint.setColor(Color.parseColor("#2610B981"));
            fillPaint.setStyle(Paint.Style.FILL);
        }

        public void setSpeed(double mbps) {
            this.currentSpeed = (float) Math.max(8.0, Math.min(300.0, mbps));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            wavePath.reset();
            float midY = h * 0.6f;
            float amp = Math.min(h * 0.35f, (currentSpeed / 200f) * (h * 0.35f) + 10f);
            wavePath.moveTo(0, h);
            for (int x = 0; x <= w; x += 15) {
                wavePath.lineTo(x, midY + (float) Math.sin((x * 0.025f) + phase) * amp);
            }
            wavePath.lineTo(w, h);
            wavePath.close();
            canvas.drawPath(wavePath, fillPaint);
            canvas.drawPath(wavePath, linePaint);
            phase += 0.18f;
            postInvalidateDelayed(35);
        }
    }

    public static class RadarSweepView extends View {
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private float angle = 0f;

        public RadarSweepView(Context context) {
            super(context);
            circlePaint.setColor(Color.parseColor("#E8F8EE"));
            circlePaint.setStyle(Paint.Style.FILL);
            arcPaint.setColor(Color.parseColor("#2ECC71"));
            arcPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - 8f;
            canvas.drawCircle(cx, cy, r, circlePaint);
            oval.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(oval, angle, 95f, true, arcPaint);
            angle = (angle + 5f) % 360f;
            postInvalidateDelayed(25);
        }
    }
}
