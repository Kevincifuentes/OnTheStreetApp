package es.deusto.onthestreetapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import static es.deusto.onthestreetapp.AddPlaceActivity.REQUEST_PERMISSION_LOCATION_UPDATES;
import static es.deusto.onthestreetapp.PlaceWidget.WIDGET_DATA_KEY;
import static es.deusto.onthestreetapp.PlaceWidget.WIDGET_IDS_KEY;

/**
 * Created by kevin on 9/4/17.
 */

//Checks for the nearest place and shows it with a notification (it also updates the widget)
public class CheckNearestPlaceService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    private Location currentLocation;
    private GoogleApiClient mGoogleApiClient;
    private SQLitePlaceHelper database;
    private ArrayList<Place> allPlaces;
    private Place lastOne;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient =  new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // And connect!
        mGoogleApiClient.connect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "Start");
        // Display the notification in the notification area
        database =
                new SQLitePlaceHelper(getApplicationContext());
        allPlaces = database.getPlaces();
        showNotification(getApplicationContext(), "Activated");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("Service", "Stop");
        // Remove the notification from the notification area
        removeNotification(getApplicationContext());
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        super.onDestroy();
    }

    /**
     * Displays a notification with id 0 in the notification area
     * @param context the current context
     * @param message the small text to display
     */
    private void showNotification(Context context, String message){
        // First, create the notification
        NotificationCompat.Builder nBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_gps_not_fixed_black_24dp)
                        .setContentTitle("Nearest place")
                        .setContentText(message);
        Notification noti = nBuilder.build();

        // Second, display the notification
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, noti);
    }

    /**
     * Removes the notification with id 0 from the notification area
     * @param context the current context
     */
    private void removeNotification(Context context){
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("Location client", "Connected");
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(currentLocation != null){
            onLocationChanged(currentLocation);
        }

        // Update if changed
        LocationRequest mLocationRequest = LocationRequest.create()
                .setInterval(3000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Location client", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("Location client", "Connection failed");
        Toast.makeText(this, R.string.msg_error_no_connection, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;

        //BROADCAST for widget change
        Intent intent = new Intent(getApplicationContext(), PlaceWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), PlaceWidget.class));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        intent.putExtra(WIDGET_IDS_KEY, ids);
        intent.putExtra(WIDGET_DATA_KEY, location);
        getApplicationContext().sendBroadcast(intent);
        //

        double distance = Double.MAX_VALUE;
        Place selected = null;
        allPlaces = database.getPlaces();
        for(Place temp: allPlaces){
            Location temp2 = new Location("Point");
            temp2.setLatitude(temp.getLat());
            temp2.setLongitude(temp.getLng());
            double current = currentLocation.distanceTo(temp2);
            if(distance > current){
                distance = current;
                selected = temp;
            }
        }

        if(selected != null){
            if(lastOne != null){
               if(lastOne.getLat() != selected.getLat() && lastOne.getLng() != selected.getLng()){
                   showNotification(getApplicationContext(), "Name: "+selected.getNombre()+" / Lat: "+ selected.getLat()+" / Lng: "+ selected.getLng());
               }
            }
            else{
                lastOne = selected;
                showNotification(getApplicationContext(), "Name: "+selected.getNombre()+" / Lat: "+ selected.getLat()+" / Lng: "+ selected.getLng());
            }
        }
    }
}
