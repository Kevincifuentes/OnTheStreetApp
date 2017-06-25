package es.deusto.onthestreetapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static es.deusto.onthestreetapp.AddPlaceActivity.REQUEST_PERMISSION_LOCATION_UPDATES;

public class MainActivity extends ListActivity implements SearchView.OnQueryTextListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    public static final String SHOW_PLACE_OBJ = "show_obj";
    private int ADD_PLACE=1;
    private int SETTINGS_PLACE = 8;
    public static int SHOW_PLACE =2;

    private ArrayList<Place> allPlaces;
    private ListView listView;
    private Context context;
    private SQLitePlaceHelper database;
    private ActionMode mActionMode = null;
    private static ArrayList<Place> mPlaces;
    private CustomAdapter mAdapter;

    //Search
    private SearchView searchView;
    private static int posicion =0;

    //Long click at the list
    public static int longClickedPos = -1;
    private int longClickedPlace = -1;

    //Search for location
    private Location currentLocation;
    private GoogleApiClient mGoogleApiClient;

    //Search for a radius
    private static int radius = 100;
    public static boolean checked = false;

    //Preferences
    private SharedPreferences sharedPrefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Preferences checking
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean serviceOn = sharedPrefs.getBoolean("locationService", false);
        if(serviceOn){
            Intent i = new Intent(getApplicationContext(), CheckNearestPlaceService.class);
            getApplicationContext().startService(i);
        }

        radius = Integer.parseInt( sharedPrefs.getString("radiusPlace", "100"));
        Log.i("RADIUS", radius+"");

        //Necessary information
        context = getApplicationContext();
        database = new SQLitePlaceHelper(context);

        //Setting the listView
        listView = (ListView)findViewById(android.R.id.list);
        listView.setEmptyView(findViewById( R.id.empty_places_view ));
        populateListPlaces();

        mAdapter = new CustomAdapter(mPlaces, this);
        listView.setAdapter(mAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            // Called when the user long-clicks an item on the list
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View row, int position, long rowid) {
                if (mActionMode != null) {
                    return false;
                }

                // Important: to marked the editing row as activated
                getListView().setItemChecked(position, true);
                longClickedPos = position;
                longClickedPlace = position;
                row.setBackgroundColor(Color.rgb(118, 172, 0));


                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = MainActivity.this.startActionMode(mActionModeCallback);
                return true;
            }
        });

        //Nearby places checkbox
        CheckBox repeatChkBx = (CheckBox) findViewById( R.id.cb_near );
        repeatChkBx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if ( isChecked )
                {
                    checked = true;
                    mGoogleApiClient =  new GoogleApiClient.Builder(MainActivity.this)
                            .addConnectionCallbacks(MainActivity.this)
                            .addOnConnectionFailedListener(MainActivity.this)
                            .addApi(LocationServices.API)
                            .build();

                    // connect
                    mGoogleApiClient.connect();

                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        if(currentLocation != null){
                            Log.i("Current", currentLocation.toString());

                            //Comprobar distancia
                            filterPlaces();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), getString(R.string.checking_location), Toast.LENGTH_LONG).show();
                        }
                    }
                    else{
                        ActivityCompat.requestPermissions(getParent(),
                                new String[] {
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                }, REQUEST_PERMISSION_LOCATION_UPDATES);
                    }

                }
                else{
                    checked = false;
                    //Disconnect location
                    mGoogleApiClient.disconnect();
                    mPlaces.removeAll(mPlaces);
                    for(Place temp : allPlaces){
                        mPlaces.add(temp);
                    }
                    mAdapter.notifyDataSetChanged();
                    allPlaces = database.getPlaces();
                }

            }
        });

        mAdapter.notifyDataSetChanged();


        //Search functionality
        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);

        searchView = (SearchView)findViewById(R.id.search_view);

        searchView.setSearchableInfo(searchManager.
                getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        //Setting preference configuration
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.places_action, menu);
            return true;
        }

        // Called when the user enters the action mode
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Disable the list to avoid selecting other elements while editing one
            listView.setEnabled(false);
            return true; // Return false if nothing is done
        }


        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.mnu_places_edit:
                    //Edit option
                    mode.finish();
                    Intent editPlaceIntent = new Intent(MainActivity.this, EditPlaceActivity.class);
                    editPlaceIntent.putExtra(ShowPlaceActivity.EDIT_PLACE_OBJ, mPlaces.get(longClickedPlace));
                    startActivityForResult(editPlaceIntent, ShowPlaceActivity.EDIT_PLACE);
                    return true;
                case R.id.mnu_places_delete:
                    //Delete place
                    mode.finish();
                    new AlertDialog.Builder(MainActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.deleting_place_title) + mPlaces.get(longClickedPlace).getNombre())
                            .setMessage(getString(R.string.deleting_place_msg)+  mPlaces.get(longClickedPlace).getNombre() + "?")
                            .setPositiveButton(R.string.positive_button_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Place temp = mPlaces.remove(longClickedPlace);
                                    database.deletePlace(temp);
                                    mAdapter.notifyDataSetChanged();
                                }

                            })
                            .setNegativeButton("No", null)
                            .show();
                    return true;
                default:
                    mode.finish();
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Re-enable the list after edition
            mActionMode = null;
            listView.setEnabled(true);
            listView.getChildAt(longClickedPos).setBackgroundColor(Color.rgb(255,255,255));
            longClickedPos = -1;


        }
    };

    private void populateListPlaces() {
        //Populate list of places
        mPlaces = database.getPlaces();
        allPlaces = database.getPlaces();

    }

    //Valid to select an option of the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Take the id of the selected option
        int id = item.getItemId();
        if (id == R.id.add_place) {
            Intent intent = new Intent(this, AddPlaceActivity.class);
            startActivityForResult(intent, ADD_PLACE);
            return true;
        }
        else if (id == R.id.menu_settings){
            startActivityForResult(new Intent(this, MySettingsActivity.class), SETTINGS_PLACE);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.places_main, menu);
        return true;
    }

    //Method triggered when clicked one item
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Place selected = (Place) l.getAdapter().getItem(position);
        Log.i("Se ha pulsado:", position+"");
        posicion = mPlaces.indexOf(selected);

        //OnClick show place
        Intent showPlaceIntent = new Intent(this, ShowPlaceActivity.class);
        showPlaceIntent.putExtra(SHOW_PLACE_OBJ, mPlaces.get(posicion));
        startActivityForResult(showPlaceIntent, SHOW_PLACE);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //Filter by the inserted text
        mAdapter.getFilter().filter(newText);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        searchView.setQuery("", false);
        searchView.clearFocus();
        if (requestCode == ADD_PLACE){
            //Add new place result
            if(resultCode == Activity.RESULT_OK){
                Bundle bundle = data.getExtras();
                Place p = (Place)bundle.getSerializable(AddPlaceActivity.ADD_PLACE_OBJ);
                int id = database.addPlace(p);
                p.setId(id);
                mPlaces.add(p);
                allPlaces = database.getPlaces();
                mAdapter.notifyDataSetChanged();
            }
        }
        else if (requestCode == SHOW_PLACE){
            //Show place result
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra(ShowPlaceActivity.DELETE);
                if(result != null){
                    //This means that probably the user selected delete option inside ShowPlace activity
                    if(result.equals("DELETE")){
                        Log.i("SELECCION", "El usuario quiere eliminar el lugar "+ posicion);
                        Place temp = mPlaces.remove(posicion);
                        database.deletePlace(temp);
                        allPlaces = database.getPlaces();
                        mAdapter.notifyDataSetChanged();
                    }
                }
                else{
                    //We check if the user has changed the place
                    boolean modified = data.getBooleanExtra(ShowPlaceActivity.MODIFIED, false);
                    if(modified){
                        Place p = database.getPlace(mPlaces.get(posicion).getId());
                        Log.i("PLACE", p.toString());
                        mPlaces.set(posicion, p);
                        allPlaces = database.getPlaces();
                        Log.i("Aqui", "ESTOY");
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }

        }
        else if (requestCode == ShowPlaceActivity.EDIT_PLACE){
            //If the result cames from EDIT
            if(resultCode == Activity.RESULT_OK){
                Bundle bundle = data.getExtras();
                Place p = (Place)bundle.getSerializable(ShowPlaceActivity.EDIT_PLACE_OBJ);
                database.updatePlace(p);
                mPlaces.set(posicion, p);
                allPlaces.set(posicion,p);
                mAdapter.notifyDataSetChanged();
            }
        }
        else if (requestCode == SETTINGS_PLACE ){
            //See if preferences changed
            int temp = Integer.parseInt( sharedPrefs.getString("radiusPlace", "100"));
            if(temp != radius){
                radius = temp;
                if(checked){
                    if(currentLocation != null){
                        onLocationChanged(currentLocation);
                    }
                }
            }

            Log.i("RADIUS", radius+"");
        }
    }

    //Permits to show the image when clicked in the icon
    public void showImage(ImageView preview) {
        final Dialog nagDialog = new Dialog(MainActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(false);
        nagDialog.setContentView(R.layout.preview_image);

        Button btnClose = (Button)nagDialog.findViewById(R.id.btnIvClose);
        ImageView ivPreview = (ImageView)nagDialog.findViewById(R.id.iv_preview_image);
        ivPreview.setImageDrawable(preview.getDrawable());
        ivPreview.setAdjustViewBounds(true);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                nagDialog.dismiss();
            }
        });
        nagDialog.show();
    }

    public void onConnected(Bundle arg0) {
        Log.i("Location client", "Connected");
        // Request location updates
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        checkLocationPermission();
        if(currentLocation != null)
        {
            filterPlaces();
        }
    }

    public void checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If there is a known location
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            // Update if changed
            LocationRequest mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, REQUEST_PERMISSION_LOCATION_UPDATES);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Location client", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Log.i("Location client", "Connection failed");
        Toast.makeText(this, R.string.msg_error_no_connection, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("Location Changed ", location.toString());
        this.currentLocation = location;

        filterPlaces();
    }

    public void filterPlaces(){
        //Filtering the list
        mPlaces.removeAll(mPlaces);
        for(Place temp: allPlaces){
            Location temp2 = new Location("Point");
            temp2.setLatitude(temp.getLat());
            temp2.setLongitude(temp.getLng());

            //Si es menor que x metros
            double distance = currentLocation.distanceTo(temp2);
            if(distance <= radius){
                Log.i("Distance", distance+"");
                temp.setComparedDistance(distance);
                mPlaces.add(temp);
            }

            //Ordenar
            Collections.sort(mPlaces, new Comparator<Place>() {
                @Override
                public int compare(Place p1, Place p2) {
                    return Double.compare(p1.getComparedDistance(), p2.getComparedDistance());
                }
            });

            mAdapter.notifyDataSetChanged();
            allPlaces = database.getPlaces();
        }
    }

    public static void getPosicion(Place dataModel) {
        posicion = mPlaces.indexOf(dataModel);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent i = new Intent(getApplicationContext(), CheckNearestPlaceService.class);
        getApplicationContext().stopService(i);
    }
}
