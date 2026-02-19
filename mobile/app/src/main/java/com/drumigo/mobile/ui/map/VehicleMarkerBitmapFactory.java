package com.drumigo.mobile.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public final class VehicleMarkerBitmapFactory {

    private VehicleMarkerBitmapFactory() {
    }

    public static Bitmap createStatusPin(
        @NonNull Context context,
        @DrawableRes int iconResId,
        @ColorInt int fillColor,
        @ColorInt int iconTintColor
    ) {
        int width = dp(context, 48);
        int height = dp(context, 60);
        int circleSize = dp(context, 40);
        int iconSize = dp(context, 20);
        float strokeWidth = dp(context, 2f);

        float centerX = width / 2f;
        float centerY = dp(context, 20);
        float radius = circleSize / 2f;
        float pointerTop = centerY + radius - dp(context, 1f);
        float pointerBottom = height - dp(context, 4);
        float pointerHalfWidth = dp(context, 8f);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(55, 0, 0, 0));
        canvas.drawCircle(centerX, centerY + dp(context, 1f), radius, shadowPaint);

        Path shadowPointer = new Path();
        shadowPointer.moveTo(centerX - pointerHalfWidth, pointerTop + dp(context, 1f));
        shadowPointer.lineTo(centerX + pointerHalfWidth, pointerTop + dp(context, 1f));
        shadowPointer.lineTo(centerX, pointerBottom + dp(context, 1f));
        shadowPointer.close();
        canvas.drawPath(shadowPointer, shadowPaint);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(fillColor);

        Path pointerPath = new Path();
        pointerPath.moveTo(centerX - pointerHalfWidth, pointerTop);
        pointerPath.lineTo(centerX + pointerHalfWidth, pointerTop);
        pointerPath.lineTo(centerX, pointerBottom);
        pointerPath.close();
        canvas.drawPath(pointerPath, fillPaint);
        canvas.drawCircle(centerX, centerY, radius, fillPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius - strokeWidth / 2f, strokePaint);

        Drawable iconDrawable = ContextCompat.getDrawable(context, iconResId);
        if (iconDrawable != null) {
            Drawable wrapped = DrawableCompat.wrap(iconDrawable.mutate());
            DrawableCompat.setTint(wrapped, iconTintColor);
            int left = Math.round(centerX - iconSize / 2f);
            int top = Math.round(centerY - iconSize / 2f);
            wrapped.setBounds(left, top, left + iconSize, top + iconSize);
            wrapped.draw(canvas);
        }
        return bitmap;
    }

    private static int dp(@NonNull Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private static float dp(@NonNull Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
