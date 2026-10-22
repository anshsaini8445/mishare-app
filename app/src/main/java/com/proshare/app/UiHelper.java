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

    // 120fps Makkhan-Smooth Touch Bounce Animation + Smart TV Focus
    @SuppressLint("ClickableViewAccessibility")
    public static void attachSmoothBounce(View v) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnTouchListener((view, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.93f).scaleY(0.93f).setDuration(90).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
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
        b.setBackground(box(bgHex, 60));
        attachSmoothBounce(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 132);
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

    // CUSTOM VECTOR ICON: SEND PAPER PLANE OR RECEIVE ARROW (Like Video 00:03)
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
                // Crisp Paper Plane Vector
                iconPath.moveTo(cx - r * 0.42f, cy + r * 0.05f);
                iconPath.lineTo(cx + r * 0.42f, cy - r * 0.38f);
                iconPath.lineTo(cx + r * 0.08f, cy + r * 0.42f);
                iconPath.lineTo(cx - r * 0.08f, cy + r * 0.14f);
                iconPath.close();
                canvas.drawPath(iconPath, iconPaint);
            } else {
                // Crisp Receive Down Arrow Vector
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

    // CUSTOM VECTOR QR LASER SCANNER FRAME (Like Video 00:10)
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
            int w = getWidth();
            int h = getHeight();
            int len = 55;

            // 4 Green Corners
            canvas.drawLine(10, 10, 10 + len, 10, cornerPaint);
            canvas.drawLine(10, 10, 10, 10 + len, cornerPaint);

            canvas.drawLine(w - 10 - len, 10, w - 10, 10, cornerPaint);
            canvas.drawLine(w - 10, 10, w - 10, 10 + len, cornerPaint);

            canvas.drawLine(10, h - 10 - len, 10, h - 10, cornerPaint);
            canvas.drawLine(10, h - 10, 10 + len, h - 10, cornerPaint);

            canvas.drawLine(w - 10 - len, h - 10, w - 10, h - 10, cornerPaint);
            canvas.drawLine(w - 10, h - 10 - len, w - 10, h - 10, cornerPaint);

            // Animated Green Laser Line
            canvas.drawLine(25, laserY, w - 25, laserY, laserPaint);
            laserY += 7f;
            if (laserY > h - 25) laserY = 25f;
            postInvalidateDelayed(25);
        }
    }

    // LIVE SPEED WAVE GRAPH
    public static class LiveSpeedGraphView extends View {
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path wavePath = new Path();
        private float phase = 0f;
        private float currentSpeed = 10f;

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
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            wavePath.reset();
            float midY = h * 0.6f;
            float amp = Math.min(h * 0.35f, (currentSpeed / 200f) * (h * 0.35f) + 10f);

            wavePath.moveTo(0, h);
            for (int x = 0; x <= w; x += 15) {
                float y = midY + (float) Math.sin((x * 0.025f) + phase) * amp;
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

    // SMOOTH GREEN RADAR SWEEP (Like Video 00:10 Bottom Sheet)
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
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - 8f;

            canvas.drawCircle(cx, cy, r, circlePaint);
            oval.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(oval, angle, 95f, true, arcPaint);

            angle = (angle + 5f) % 360f;
            postInvalidateDelayed(25);
        }
    }
}
