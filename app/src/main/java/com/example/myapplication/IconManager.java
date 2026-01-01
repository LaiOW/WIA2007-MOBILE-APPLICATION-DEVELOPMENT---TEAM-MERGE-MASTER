package com.example.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

/**
 * IconManager handles all icon-related operations for the map
 * Including creating custom icons, setting marker icons, and custom info windows
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

    /**
     * Custom compact info window for SOS markers with route button
     */
    public static class CustomInfoWindow extends MarkerInfoWindow {

        private Runnable onRouteClick;
        private Context context;

        public CustomInfoWindow(MapView mapView, Context context) {
            super(R.layout.bonuspack_bubble, mapView);
            this.context = context;
        }

        public void setOnRouteClickListener(Runnable listener) {
            this.onRouteClick = listener;
        }

        @Override
        public void onOpen(Object item) {
            try {
                super.onOpen(item);

                if (!(item instanceof Marker)) {
                    return;
                }

                Marker marker = (Marker) item;

                if (mView == null) {
                    return;
                }

                // Set title
                TextView txtTitle = mView.findViewById(R.id.bubble_title);
                TextView txtDescription = mView.findViewById(R.id.bubble_description);
                TextView txtSubDescription = mView.findViewById(R.id.bubble_subdescription);
                TextView txtMoreInfo = mView.findViewById(R.id.bubble_moreinfo);

                if (txtTitle != null && marker.getTitle() != null) {
                    txtTitle.setText(marker.getTitle());
                }

                if (txtDescription != null && txtSubDescription != null && marker.getSnippet() != null) {
                    String[] lines = marker.getSnippet().split("\n");
                    StringBuilder desc = new StringBuilder();
                    StringBuilder subDesc = new StringBuilder();

                    for (String line : lines) {
                        if (line.startsWith("User:")) {
                            desc.append(line);
                        } else if (line.startsWith("Time:")) {
                            if (desc.length() > 0) {
                                desc.append("\n");
                            }
                            desc.append(line);
                        } else if (line.startsWith("Distance:")) {
                            subDesc.append(line);
                        }
                    }

                    txtDescription.setText(desc.toString());
                    txtSubDescription.setText(subDesc.toString());
                }

                // The hint text is already set in XML, no need to modify it

                android.util.Log.d("CustomInfoWindow", "InfoWindow setup complete");

            } catch (Exception e) {
                // Prevent crash - log error
                android.util.Log.e("CustomInfoWindow", "Error in onOpen: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onClose() {
            try {
                super.onClose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
