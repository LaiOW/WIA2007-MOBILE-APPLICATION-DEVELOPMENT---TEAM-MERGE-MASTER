package com.example.myapplication;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.example.myapplication.SupabaseManager;
import com.example.myapplication.SOSCall;

public class SOSNotificationService extends Service {

    private static final String TAG = "SOSService";
    private static final String CHANNEL_ID = "SOS_CHANNEL";
    private static final int NOTIFICATION_ID = 12345;
    private static final int ALERT_NOTIFICATION_ID = 999;
    private LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        startForeground(NOTIFICATION_ID, getNotification("Monitoring for nearby SOS calls..."));

        SupabaseManager.INSTANCE.subscribeToSOSCalls(sosCall -> {
            Log.d(TAG, "New SOS Call Received: " + sosCall.getUsername());
            checkDistanceAndNotify(sosCall);
        });

        // Request location updates to ensure we have a fresh location
        requestLocationUpdates();

        return START_STICKY;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions missing");
            return;
        }

        try {
            // Request updates from both providers to increase chance of getting a location
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 100, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 100, locationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting location updates", e);
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            // We just need the system to have a recent location, we don't need to do anything specific here
            // unless we want to cache it ourselves.
            Log.d(TAG, "Location updated: " + location.getLatitude() + "," + location.getLongitude());
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(@NonNull String provider) {}
        @Override public void onProviderDisabled(@NonNull String provider) {}
    };

    private void checkDistanceAndNotify(SOSCall sosCall) {
        // Commented out self-check for testing purposes
        /*
        String currentUserEmail = SupabaseManager.INSTANCE.getCurrentUserEmail();
        if (currentUserEmail != null && currentUserEmail.contains(sosCall.getUsername())) {
            Log.d(TAG, "Ignoring own SOS call");
            return;
        }
        */

        Location userLocation = getLastKnownLocation();
        if (userLocation == null) {
            Log.e(TAG, "User location is null, cannot check distance");
            // Fallback: Notify anyway if location is unknown? For safety, maybe yes.
            // showSOSAlert(sosCall, -1);
            return;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(), userLocation.getLongitude(),
                sosCall.getX_coordinate(), sosCall.getY_coordinate(),
                results
        );

        float distanceInMeters = results[0];
        Log.d(TAG, "Distance to SOS: " + distanceInMeters + " meters");

        if (distanceInMeters < 5000) { // 5km radius
            showSOSAlert(sosCall, distanceInMeters);
        } else {
            Log.d(TAG, "SOS call too far away");
        }
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLoc != null && netLoc != null) {
            return gpsLoc.getTime() > netLoc.getTime() ? gpsLoc : netLoc;
        }
        return gpsLoc != null ? gpsLoc : netLoc;
    }

    private void showSOSAlert(SOSCall sosCall, float distance) {
        Log.d(TAG, "Showing SOS Alert");
        String title = "SOS Alert Nearby!";
        String message = String.format("User %s needs help %.2f km away!", sosCall.getUsername(), distance / 1000);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL) // Sound, vibration, lights
                .setAutoCancel(true)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(ALERT_NOTIFICATION_ID + (int)System.currentTimeMillis(), notification);
        }
    }

    private Notification getNotification(String content) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safety App Service")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SOS Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for nearby SOS calls");
            channel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}