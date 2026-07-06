package com.example.mpos.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

public class LineChartView extends View {

    private long[] values;
    private String[] labels;

    private final Paint gridPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int COLOR_LINE  = 0xFF1A73E8;
    private static final int COLOR_FILL  = 0x201A73E8;
    private static final int COLOR_GRID  = 0xFFF1F5F9;
    private static final int COLOR_LABEL = 0xFF94A3B8;

    private static final int Y_GRID_LINES = 4;

    public LineChartView(Context ctx) { super(ctx); init(); }
    public LineChartView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public LineChartView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); init(); }

    private void init() {
        gridPaint.setColor(COLOR_GRID);
        gridPaint.setStrokeWidth(1f);

        linePaint.setColor(COLOR_LINE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint.setColor(COLOR_FILL);
        fillPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(dp(9));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setAntiAlias(true);

        yLabelPaint.setColor(COLOR_LABEL);
        yLabelPaint.setTextSize(dp(9));
        yLabelPaint.setTextAlign(Paint.Align.RIGHT);
        yLabelPaint.setAntiAlias(true);
    }

    public void setData(long[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (values == null || values.length < 2) return;

        int w = getWidth();
        int h = getHeight();
        float yLabelWidth = dp(38);
        float xPad = dp(8);
        float topPad = dp(10);
        float bottomPad = dp(22);

        float chartLeft  = yLabelWidth;
        float chartRight = w - xPad;
        float chartTop   = topPad;
        float chartBot   = h - bottomPad;
        float chartW = chartRight - chartLeft;
        float chartH = chartBot - chartTop;

        long maxVal = 0;
        for (long v : values) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;
        long gridStep = niceStep(maxVal, Y_GRID_LINES);
        long gridMax  = gridStep * Y_GRID_LINES;

        // Draw horizontal grid lines + Y labels
        for (int i = 0; i <= Y_GRID_LINES; i++) {
            long gridVal = gridStep * i;
            float y = chartBot - (chartH * gridVal / gridMax);
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
            canvas.drawText(compact(gridVal), yLabelWidth - dp(4), y + dp(3.5f), yLabelPaint);
        }

        // Compute point coordinates
        int n = values.length;
        float[] px = new float[n];
        float[] py = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = chartLeft + chartW * i / (n - 1);
            py[i] = chartBot - (chartH * values[i] / gridMax);
        }

        // Fill path (gradient-like solid fill under line, close to bottom)
        Path fillPath = new Path();
        fillPath.moveTo(px[0], chartBot);
        fillPath.lineTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            float cpx = (px[i - 1] + px[i]) / 2f;
            fillPath.cubicTo(cpx, py[i - 1], cpx, py[i], px[i], py[i]);
        }
        fillPath.lineTo(px[n - 1], chartBot);
        fillPath.close();

        // Apply vertical gradient fill
        fillPaint.setShader(new LinearGradient(
            0, chartTop, 0, chartBot,
            0x501A73E8, 0x001A73E8, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Line path (smooth cubic bezier)
        Path linePath = new Path();
        linePath.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            float cpx = (px[i - 1] + px[i]) / 2f;
            linePath.cubicTo(cpx, py[i - 1], cpx, py[i], px[i], py[i]);
        }
        canvas.drawPath(linePath, linePaint);

        // X-axis labels — show at most 7 to avoid overlap
        int step = Math.max(1, (int) Math.ceil(n / 7.0));
        for (int i = 0; i < n; i += step) {
            if (labels != null && i < labels.length) {
                canvas.drawText(labels[i], px[i], h - dp(4), labelPaint);
            }
        }
        // Always draw last label
        if (labels != null && n > 0 && (n - 1) % step != 0) {
            canvas.drawText(labels[n - 1], px[n - 1], h - dp(4), labelPaint);
        }
    }

    private long niceStep(long max, int lines) {
        long rawStep = max / lines;
        if (rawStep == 0) return 1;
        long magnitude = (long) Math.pow(10, Math.floor(Math.log10(rawStep)));
        long[] nice = {1, 2, 5, 10};
        long best = magnitude;
        for (long n : nice) {
            long candidate = n * magnitude;
            if (candidate >= rawStep) { best = candidate; break; }
        }
        return best;
    }

    private String compact(long v) {
        if (v >= 1_000_000L) return String.format(Locale.getDefault(), "%.1fM", v / 1_000_000f);
        if (v >= 1_000L)     return String.format(Locale.getDefault(), "%.0fK", v / 1_000f);
        return String.valueOf(v);
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
