package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.osmdroid.util.GeoPoint;
import java.util.List;
import java.util.Locale;

public class NearestSOSDialog {

    public static class SOSCallWithDistance {
        public SOSCall sosCall;
        public double distance;

        public SOSCallWithDistance(SOSCall sosCall, double distance) {
            this.sosCall = sosCall;
            this.distance = distance;
        }
    }

    public interface OnSOSItemClickListener {
        void onSOSItemClick(SOSCall sosCall, GeoPoint location);
    }

    // Custom adapter for better visual presentation
    private static class SOSAdapter extends ArrayAdapter<SOSCallWithDistance> {
        private final Context context;
        private final List<SOSCallWithDistance> items;

        public SOSAdapter(Context context, List<SOSCallWithDistance> items) {
            super(context, R.layout.item_nearest_sos, items);
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_nearest_sos, parent, false);
            }

            SOSCallWithDistance item = items.get(position);

            TextView tvUsername = convertView.findViewById(R.id.tvSOSUsername);
            TextView tvDistance = convertView.findViewById(R.id.tvSOSDistance);
            TextView tvTime = convertView.findViewById(R.id.tvSOSTime);

            tvUsername.setText(String.format(Locale.getDefault(), "%d. %s", position + 1, item.sosCall.getUsername()));
            tvDistance.setText(String.format(Locale.getDefault(), "📍 %.2f km away", item.distance / 1000.0));
            tvTime.setText(String.format(Locale.getDefault(), "🕐 %s", item.sosCall.getTime()));

            return convertView;
        }
    }

    public static void show(Context context,
                           List<SOSCallWithDistance> nearest5,
                           GeoPoint userLocation,
                           OnSOSItemClickListener listener) {

        // Build title with user location
        String dialogTitle = "📍 5 Nearest SOS Calls\nTap to navigate";

        // Create custom adapter
        SOSAdapter adapter = new SOSAdapter(context, nearest5);

        // Show clickable dialog with list
        new AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setAdapter(adapter, (dialog, which) -> {
                // User clicked on an SOS call
                SOSCallWithDistance selectedItem = nearest5.get(which);
                GeoPoint sosPoint = new GeoPoint(
                    selectedItem.sosCall.getX_coordinate(),
                    selectedItem.sosCall.getY_coordinate()
                );

                // Notify listener
                if (listener != null) {
                    listener.onSOSItemClick(selectedItem.sosCall, sosPoint);
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }
}

