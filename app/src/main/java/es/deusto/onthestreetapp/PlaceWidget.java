package es.deusto.onthestreetapp;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import static android.content.Context.LOCATION_SERVICE;
import static es.deusto.onthestreetapp.MainActivity.SHOW_PLACE;
import static es.deusto.onthestreetapp.MainActivity.SHOW_PLACE_OBJ;


/**
 * Implementation of App Widget functionality.
 */
public class PlaceWidget extends AppWidgetProvider{

    boolean hasLocation = false;
    private Location currentLocation;
    private LocationManager locationManager;
    private SQLitePlaceHelper database;
    private Context mContext;

    public static final String WIDGET_IDS_KEY ="mywidgetproviderwidgetids";
    public static final String WIDGET_DATA_KEY ="mywidgetproviderwidgetdata";
    public static final String WIDGET_COLOR_KEY="mywidgetproviderwidgetcolor";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(WIDGET_IDS_KEY)) {
            int[] ids = intent.getExtras().getIntArray(WIDGET_IDS_KEY);
            if (intent.hasExtra(WIDGET_DATA_KEY)) {
                Object data = intent.getExtras().getParcelable(WIDGET_DATA_KEY);
                this.update(context, AppWidgetManager.getInstance(context), ids, data);
            } else {
                if(intent.hasExtra(WIDGET_COLOR_KEY)){
                    String color = intent.getExtras().getString(WIDGET_COLOR_KEY, context.getResources().getStringArray(R.array.colores)[0]);
                    this.changeColor(context, AppWidgetManager.getInstance(context), ids, color);
                }
                else{
                    this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
                }
            }
        } else super.onReceive(context, intent);
    }

    public void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, Object data) {
        //Get the color for the widget
        SharedPreferences shared  = PreferenceManager.getDefaultSharedPreferences(context);
        String colorType = shared.getString("widget_preference", context.getResources().getStringArray(R.array.colores)[0]);

        Log.i("WIDGET", "AQUIIIIIIII");
        currentLocation = (Location) data;
        locationManager  = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if(currentLocation == null){
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if(currentLocation == null){
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        database = new SQLitePlaceHelper(context);
        ArrayList<Place> places = database.getPlaces();
        float minDistance = Float.MAX_VALUE;
        Place minPlace = null;
        Log.i("Aqui", "sí");


        if(currentLocation != null){
            Log.i("ENTROOO","SIII");
            for(Place temp: places){
                Location temp2 = new Location("Point");
                temp2.setLatitude(temp.getLat());
                temp2.setLongitude(temp.getLng());

                //Si es menor que 100 metros
                float distancecurrent = currentLocation.distanceTo(temp2);
                if(distancecurrent < minDistance){
                    minDistance = distancecurrent;
                    minPlace = temp;
                }
            }

            // We must iterate all the widget instances
            for(int i = 0; i < appWidgetIds.length; i++){
                int widgetId = appWidgetIds[i];
                // Set the image and update the widget
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.place_widget);
                views.setTextViewText(R.id.txt_nearest_name, minPlace.getNombre());
                views.setTextViewText(R.id.txt_nearest_address, minPlace.getDireccion().replace("\n", " "));

                if(colorType.equals(context.getResources().getStringArray(R.array.colores)[0])){
                    //Verde
                    views.setInt(R.id.relative_widget, "setBackgroundResource", R.drawable.widget_shape_green);
                }
                else if(colorType.equals(context.getResources().getStringArray(R.array.colores)[1])){
                    //Rojo
                    views.setInt(R.id.relative_widget, "setBackgroundResource", R.drawable.widget_shape_red);
                }
                else if(colorType.equals(context.getResources().getStringArray(R.array.colores)[2])){
                    //Azul
                    views.setInt(R.id.relative_widget, "setBackgroundResource", R.drawable.widget_shape_blue);
                }

                Intent showPlaceIntent = new Intent(context, ShowPlaceActivity.class);
                showPlaceIntent.putExtra(SHOW_PLACE_OBJ, minPlace);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, showPlaceIntent, 0);
                views.setOnClickPendingIntent(R.id.layout_widget, pendingIntent);

                appWidgetManager.updateAppWidget(widgetId, views);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, null);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public void changeColor(Context context, AppWidgetManager appWidgetManager,
                             int[] appWidgetIds, String colorType) {
        // We must iterate all the widget instances
        for(int i = 0; i < appWidgetIds.length; i++){
            int widgetId = appWidgetIds[i];
            // Set the image and update the widget
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.place_widget);

            if(colorType.equals(context.getResources().getStringArray(R.array.colores)[0])){
                //Verde
                views.setInt(R.id.relative_widget, "setBackgroundResource", R.drawable.widget_shape_green);
            }
            else if(colorType.equals(context.getResources().getStringArray(R.array.colores)[1])){
                //Rojo
                views.setInt(R.id.relative_widget, "setBackgroundResource", R.drawable.widget_shape_red);
            }
            else if(colorType.equals(context.getResources().getStringArray(R.array.colores)[2])){
                //Azul
                views.setInt(R.id.relative_widget, "setBackgroundResource", R.drawable.widget_shape_blue);
            }
            appWidgetManager.updateAppWidget(widgetId, views);
        }


    }
}


