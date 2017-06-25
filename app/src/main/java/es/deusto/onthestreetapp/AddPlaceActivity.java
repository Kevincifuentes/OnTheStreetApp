package es.deusto.onthestreetapp;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class AddPlaceActivity extends Activity implements CallBackHTTPInterface, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    //Keys
	public static final String PLACE = "place";
	public static final String ADD_PLACE_OBJ = "PLACE_OBJ";
    public static final int REQUEST_PERMISSION_LOCATION_UPDATES = 1;

    private boolean connected = false;
	public String urlCall ="";
	public Place adding;
    public boolean firstTime = true;
    private boolean posicionActual = false;
    private String obtainedAddress;
    private boolean current;

    //Location search
    private GoogleApiClient mGoogleApiClient;
    private Location location = null;
    private AddressResultReceiver mResultReceiver;

    //View
    private EditText direccion;
    public ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_place);

        //Actionbar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        direccion = (EditText) findViewById(R.id.input_direccion);

        //Check preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //Check if add place with current position
        current = sharedPrefs.getBoolean("addCurrent", false);
        if(current){
            findViewById(R.id.btn_localizar).setVisibility(View.INVISIBLE);
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.item_detail, menu);
		return true;
	}

    @Override
    protected void onStart() {
        super.onStart();
        //Cuando se ve me conecto
        connectToGooglePlayServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Cuando se deja de ver me desconecto
        disconnectFromGooglePlayServices();
    }
	
	public void addPlace(View view){
        //If the user clicked to add current position
        if(!posicionActual){
            connected = isConnected(getApplicationContext());
            //If is connected to the internet and is first time
            //If it isn't the first time, places will be added as default
            if(!connected && firstTime){
                firstTime = false;
                buildDialog(this, getString(R.string.internet_error_title), getString(R.string.internet_error_msg)).show();
            }
            else{
                dialog = ProgressDialog.show(this, getString(R.string.loading_title),
                        getString(R.string.loading_msg), true);

                dialog.show();

                String desc = ((EditText) findViewById(R.id.input_desc)).getText().toString();
                String dir = ((EditText) findViewById(R.id.input_direccion)).getText().toString();
                String nom = ((EditText) findViewById(R.id.input_nombre)).getText().toString();

                Place p = new Place(nom, dir, desc);
                adding = p;

                String dirFormat = dir.replace(" ", "+");

                if(connected){
                    urlCall ="https://maps.googleapis.com/maps/api/geocode/json?address="+dirFormat+"&sensor=true_or_false&key=AIzaSyBtmhv-d87g5sOa3P2WfH8pt6q0t5NsQhA";

                    new RequestLatLngTask().execute();
                }
                else{
                    finishAdding(null);
                }
            }
        }
        else{
            //using the current location
            String desc = ((EditText) findViewById(R.id.input_desc)).getText().toString();
            String dir = ((EditText) findViewById(R.id.input_direccion)).getText().toString();
            String nom = ((EditText) findViewById(R.id.input_nombre)).getText().toString();
            Place p = new Place(nom, dir, desc);
            adding = p;
            adding.setLat(location.getLatitude());
            adding.setLng(location.getLongitude());

            Intent intentResult = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(ADD_PLACE_OBJ, adding);
            intentResult.putExtras(bundle);
            setResult(Activity.RESULT_OK, intentResult);
            finish();
        }

	}

    public void localize(View view){
        //Clicked on the button to fill the address
        Log.i("Localizar", location.toString());
        posicionActual = true;
        direccion.setText(obtainedAddress);

        direccion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                posicionActual = false;
                direccion.removeTextChangedListener(this);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    //Method to ask for a URL and get the resulting JSON
	public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

		HttpURLConnection urlConnection = null;

		URL url = new URL(urlString);

		urlConnection = (HttpURLConnection) url.openConnection();

		urlConnection.setRequestMethod("GET");
		urlConnection.setReadTimeout(10000 /* milliseconds */);
		urlConnection.setConnectTimeout(15000 /* milliseconds */);

		urlConnection.setDoOutput(true);

		urlConnection.connect();

		BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));

		char[] buffer = new char[1024];

		String jsonString = new String();

		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line+"\n");
		}
		br.close();

		jsonString = sb.toString();

		System.out.println("JSON: " + jsonString);

		return new JSONObject(jsonString);
	}

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Log.i("Location client", "Connection failed");
        Toast.makeText(this, R.string.msg_error_no_connection, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("Location Changed ", location.toString());
        this.location = location;
        startIntentService();
    }

    //AsyncTask that asks for a URL
    private class RequestLatLngTask extends AsyncTask<Void, Void, Void> {

		JSONObject resultJson;

		@Override
		protected Void doInBackground(Void... params) {
			resultJson = null;
			try {
				resultJson = getJSONObjectFromURL(urlCall);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;

		}

		@Override
		protected void onPostExecute(Void result) {
			finishAdding(resultJson);
		}

	}

    //Method to add finally a place
	public void finishAdding(JSONObject jsonObject){
        if(jsonObject!= null){
            Double lat = null, lng = null;
            boolean error = false;
            try {
                JSONArray result = jsonObject.getJSONArray("results");
                JSONObject all = result.getJSONObject(0);
                JSONObject geo = all.getJSONObject("geometry");
                JSONObject location = geo.getJSONObject("location");
                Log.i("Resultado", location.toString());
                lat = location.getDouble("lat");
                lng = location.getDouble("lng");
                Log.i("Obtained", lat + " / " + lng);
            } catch (JSONException e) {
                if(connected){
                    error = true;
                    dialog.cancel();
                    buildDialog(this, "Address error", "Inserted address doesn't exist. Try with another one.").show();
                }
            }
            if(!error){
                adding.setLat(lat);
                adding.setLng(lng);

                Intent intentResult = new Intent();
                Bundle bundle = new Bundle();
                bundle.putSerializable(ADD_PLACE_OBJ, adding);
                intentResult.putExtras(bundle);
                setResult(Activity.RESULT_OK, intentResult);
                dialog.cancel();
                finish();
            }
        }
        else{
            adding.setLat(0.0);
            adding.setLng(0.0);

            Intent intentResult = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(ADD_PLACE_OBJ, adding);
            intentResult.putExtras(bundle);
            setResult(Activity.RESULT_OK, intentResult);
            dialog.cancel();
            finish();
        }
    }

    //Selecting option from menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Cojo el identificador de la opción de menu
        int id = item.getItemId();
        if(id == android.R.id.home){
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    //Alert builder method
    public static AlertDialog buildDialog(Context c, String title, String message) {

        AlertDialog alertDialog = new AlertDialog.Builder(c).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return alertDialog;
    }

    //Method that checks if the user is connected to the internet
    public boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if((mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting())) return true;
            else return false;
        } else
            return false;
    }

    private boolean connectToGooglePlayServices(){
        // Check that Google Play Services is available
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.SUCCESS){
            return false;
        }

        // Just log Google Play Services version code to check errors.
        // If the version installed in the smartphone is lower than the version linked in the app (build.gradle) our app will not work
        Log.i("Location client","Play services version code: " + GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE);

        // Create the Google API Client
        mGoogleApiClient =  new GoogleApiClient.Builder(AddPlaceActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // And connect!
        mGoogleApiClient.connect();
        return true;
    }

    private void disconnectFromGooglePlayServices(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
            Log.i("Location client", "Disconnected");
        }
    }

    public void onConnected(Bundle arg0) {
        Log.i("Location client", "Connected");
        // Request location updates
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        checkLocationPermission();
        if(location != null)
        {
            startIntentService();
        }
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Location client", "Connection suspended");
    }


    public void checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If there is a known location
            location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

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

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            obtainedAddress = resultData.getString(Constants.RESULT_DATA_KEY);
            if(current){
                posicionActual = true;
                direccion.setText(obtainedAddress);
            }

        }
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        mResultReceiver = new AddressResultReceiver(new Handler());
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lng", location.getLongitude());
        startService(intent);
    }

}
