package es.deusto.onthestreetapp;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ShowPlaceActivity extends Activity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap map;
    private Place place;
    private MarkerOptions options;

    public static final String DELETE = "delete";
    public static final String MODIFIED = "modified";
    private boolean modified = false;
    public static final String EDIT_PLACE_OBJ = "edit_obj";
    public static final String CONTACTS_PLACE_OBJ = "contacts_obj";

    public static final int EDIT_PLACE = 3;
    public static final int MANAGE_CONTACTS = 4;
    public static final int ADD_PHOTO = 5;

    private SQLitePlaceHelper database;
    private int numberOfContacts=0;

    private ImageView image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_place);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        place = (Place) getIntent().getSerializableExtra(MainActivity.SHOW_PLACE_OBJ);

        //Set values to the corresponding TextViews
        TextView name = ((TextView) findViewById(R.id.show_name));
        name.setText(place.getNombre());

        TextView address = ((TextView) findViewById(R.id.show_address));
        address.setText(place.getDireccion().replace("\n", ", "));

        TextView desc = ((TextView) findViewById(R.id.show_description));
        desc.setText(place.getDesc());

        image = ((ImageView) findViewById(R.id.image_place));
        if(place.getImageURI() != null){
            image.setImageURI(Uri.parse(place.getImageURI()));
        }

        database = new SQLitePlaceHelper(getApplicationContext());
        numberOfContacts = database.countContacts(place.getId());

        TextView contacts = ((TextView) findViewById(R.id.number_contacts));
        contacts.setText(numberOfContacts+"");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //El inflador de menus lo recibe y lo rederiza
        //renderizo el menu vacio con el layout que he creado
        getMenuInflater().inflate(R.menu.places_show, menu);

        MenuItem mnuShare = menu.findItem(R.id.mnu_share);
        ShareActionProvider shareProv = (ShareActionProvider)
                mnuShare.getActionProvider();
        shareProv.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "¡Echalé un vistazo al lugar "+ place.getNombre()+" en la dirección "+place.getDireccion()
                +" (Latitud: "+place.getLat()+" / Longitud: "+place.getLng()+")!");
        shareProv.setShareIntent(intent);
        return true;
    }

    //Valido para seleccionar una opción del menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Cojo el identificador de la opción de menu
        int id = item.getItemId();
        if (id == R.id.delete_place) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.deleting_place_title) + place.getNombre())
                    .setMessage(getString(R.string.deleting_place_msg) + place.getNombre() + "?")
                    .setPositiveButton(R.string.positive_button_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intentResult = new Intent();
                            intentResult.putExtra(DELETE, "DELETE");
                            setResult(Activity.RESULT_OK, intentResult);
                            finish();
                        }

                    })
                    .setNegativeButton(R.string.negative_button_no, null)
                    .show();
            return true;
        } else if (id == R.id.edit_place) {
            Intent editPlaceIntent = new Intent(this, EditPlaceActivity.class);
            editPlaceIntent.putExtra(EDIT_PLACE_OBJ, place);
            startActivityForResult(editPlaceIntent, EDIT_PLACE);
            return true;

        }else if(id == android.R.id.home){
            Intent intentResult = new Intent();
            intentResult.putExtra(MODIFIED, modified);
            intentResult.putExtra(EDIT_PLACE_OBJ, place);
            setResult(Activity.RESULT_OK, intentResult);
            finish();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onMapReady(GoogleMap map) {
        Log.i("LAT/LNG", place.getLat()+" / "+ place.getLng());
        LatLng coordinates = new LatLng(place.getLat(), place.getLng());
        options = new MarkerOptions().position(coordinates).title(place.getDireccion());
        map.addMarker(options);

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(getApplicationContext());

        // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(place.getLat(), place.getLng()), 14);
        map.animateCamera(cameraUpdate);
        this.map = map;
        mapView.onResume();
    }

    public void showImage(View view){
        final Dialog nagDialog = new Dialog(ShowPlaceActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(false);
        nagDialog.setContentView(R.layout.preview_image);
        Button btnClose = (Button)nagDialog.findViewById(R.id.btnIvClose);
        ImageView ivPreview = (ImageView)nagDialog.findViewById(R.id.iv_preview_image);
        if(place.getImageURI()!= null){
            ivPreview.setImageURI(Uri.parse(place.getImageURI()));
        }
        else{
            ivPreview.setImageDrawable(((ImageView) view).getDrawable());
        }
        ivPreview.setAdjustViewBounds(true);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                nagDialog.dismiss();
            }
        });
        nagDialog.show();
    }

    public void addPhoto(View view){
        if(place.getImageURI() == null){
            Log.i("IS NULL", "YES");
            if (Build.VERSION.SDK_INT >= 23 && this.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
            else{
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, ADD_PHOTO);
            }

        }
        else{
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.adding_photo_repeat_msg) + place.getNombre())
                    .setMessage(getString(R.string.adding_photo_repeat_msg))
                    .setPositiveButton(R.string.positive_button_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, ADD_PHOTO);
                        }

                    })
                    .setNegativeButton(R.string.negative_button_no, null)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PLACE){
            if(resultCode == Activity.RESULT_OK){
                Bundle bundle = data.getExtras();
                Place p = (Place)bundle.getSerializable(EDIT_PLACE_OBJ);
                database.updatePlace(p);
                place = p;

                modified = true;

                //Update
                TextView name = ((TextView) findViewById(R.id.show_name));
                name.setText(place.getNombre());

                TextView address = ((TextView) findViewById(R.id.show_address));
                address.setText(place.getDireccion());

                TextView desc = ((TextView) findViewById(R.id.show_description));
                desc.setText(place.getDesc());

                options.position(new LatLng(place.getLat(), place.getLng()));
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(place.getLat(), place.getLng()), 14);
                map.animateCamera(cameraUpdate);
                mapView.onResume();
            }
        }
        else if (requestCode == ADD_PHOTO){
            if(resultCode == Activity.RESULT_OK){
                String realPath;
                if (Build.VERSION.SDK_INT < 11)
                    realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(this, data.getData());
                    // SDK >= 11 && SDK < 19
                else if (Build.VERSION.SDK_INT < 20)
                    realPath = RealPathUtil.getRealPathFromURI_API11to18(this, data.getData());
                    // SDK > 19 (Android 4.4)
                else
                    realPath = RealPathUtil.getRealPathFromURI_API19(this, data.getData());

                Log.i("URI", realPath);
                image.setImageURI(Uri.parse(realPath));
                place.setImageURI(realPath);
                modified = true;
                database.updatePlace(place);
            }
        }
        else if (requestCode == MANAGE_CONTACTS){
            if(resultCode == Activity.RESULT_OK){
                boolean modified = data.getBooleanExtra(MODIFIED, false);
                if(modified){
                    int numberContacts = data.getIntExtra(ContactsActivity.NUMBER_CONTACTS, -1);
                    Log.i("Number of contacts", numberContacts+"");
                    if(numberContacts != -1){
                        TextView contacts = ((TextView) findViewById(R.id.number_contacts));
                        contacts.setText(numberContacts+"");
                        numberOfContacts = numberContacts;
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length == 0){
                    // Permissions nor granted
                } else {
                    if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");

                        if(checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                            startActivityForResult(intent, ADD_PHOTO);
                        }
                    }
                }
                return;
            }
        }
    }

    public void showContacts(View v){
        Intent showPlaceIntent = new Intent(this, ContactsActivity.class);
        showPlaceIntent.putExtra(ContactsActivity.NUMBER_CONTACTS, numberOfContacts);
        showPlaceIntent.putExtra(CONTACTS_PLACE_OBJ, place);
        startActivityForResult(showPlaceIntent, MANAGE_CONTACTS);
    }
}
