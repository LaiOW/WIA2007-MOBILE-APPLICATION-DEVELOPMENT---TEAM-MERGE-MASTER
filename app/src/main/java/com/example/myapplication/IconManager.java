package com.example.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

import org.osmdroid.views.overlay.Marker;

/**
 * IconManager handles all icon-related operations for the map
 * Including creating custom icons and setting marker icons
 */
public class IconManager {

    private final Context context;
    private final Resources resources;

    public IconManager(Context context) {
        this.context = context;
        this.resources = context.getResources();
    }

    /**
     * Create a custom person icon with arrow for user location
     * @return Bitmap of the person icon with "You are here" text
     */
    public Bitmap createPersonIconWithArrow() {
        int width = 150;
        int height = 150;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Create arrow path
        Path arrowPath = new Path();
        arrowPath.moveTo(width / 2f, height / 2f);
        arrowPath.lineTo(width / 2f - 20, height / 2f + 40);
        arrowPath.lineTo(width / 2f, height / 2f - 40);
        arrowPath.lineTo(width / 2f + 20, height / 2f + 40);
        arrowPath.close();

        // Draw arrow
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, paint);

        // Draw circle at center
        paint.setColor(Color.BLUE);
        canvas.drawCircle(width / 2f, height / 2f, 15, paint);

        // Draw text
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("You are here", width / 2f, height / 2f - 50, paint);

        return bitmap;
    }

    /**
     * Get the default red alert icon for SOS markers
     * @return Drawable for alert icon
     */
    public Drawable getAlertIcon() {
        return resources.getDrawable(android.R.drawable.ic_dialog_alert, null);
    }

    /**
     * Get the gold star icon for nearest SOS markers
     * @return Drawable for star icon
     */
    public Drawable getStarIcon() {
        return resources.getDrawable(android.R.drawable.star_big_on, null);
    }

    /**
     * Set marker to use alert icon (red)
     * @param marker The marker to update
     */
    public void setAlertIcon(Marker marker) {
        marker.setIcon(getAlertIcon());
    }

    /**
     * Set marker to use star icon (gold)
     * @param marker The marker to update
     */
    public void setStarIcon(Marker marker) {
        marker.setIcon(getStarIcon());
    }

    /**
     * Set marker to use alert icon with transparency
     * @param marker The marker to update
     * @param alpha Alpha value (0.0 to 1.0)
     */
    public void setAlertIconWithAlpha(Marker marker, float alpha) {
        setAlertIcon(marker);
        marker.setAlpha(alpha);
    }

    /**
     * Set marker to use star icon with full opacity
     * @param marker The marker to update
     */
    public void setStarIconHighlighted(Marker marker) {
        setStarIcon(marker);
        marker.setAlpha(1.0f);
    }

    /**
     * Reset marker to default dimmed alert icon
     * @param marker The marker to reset
     */
    public void resetToDefaultIcon(Marker marker) {
        setAlertIconWithAlpha(marker, 0.5f);
    }

    /**
     * Highlight marker as nearest SOS call
     * @param marker The marker to highlight
     */
    public void highlightAsNearest(Marker marker) {
        setStarIconHighlighted(marker);
    }
}

