package es.deusto.onthestreetapp;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class EditPlaceActivity extends Activity {

    private Place place;
    private EditText name;
    private EditText desc;
    private EditText address;
    private EditText lat;
    private EditText lng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_place);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        place = (Place) getIntent().getSerializableExtra(ShowPlaceActivity.EDIT_PLACE_OBJ);

        Log.i("Nombre", place.getNombre());
        Log.i("Address", place.getDireccion());

        name = ((EditText) findViewById(R.id.edit_name));
        name.setText(place.getNombre());

        desc = ((EditText) findViewById(R.id.edit_desc));
        desc.setText(place.getDesc());

        address = ((EditText) findViewById(R.id.edit_address));
        address.setText(place.getDireccion());

        lat = ((EditText) findViewById(R.id.edit_lat));
        lat.setText(place.getLat()+"");

        lng = ((EditText) findViewById(R.id.edit_lng));
        lng.setText(place.getLng()+"");

    }

    public void editPlace(View view) {
        String name = ((EditText) findViewById(R.id.edit_name)).getText().toString();
        String desc = ((EditText) findViewById(R.id.edit_desc)).getText().toString();
        String address =  ((EditText) findViewById(R.id.edit_address)).getText().toString();
        boolean exception = false;
        Double latd=0.0, lngd =0.0;
        try {
            latd = Double.parseDouble(lat.getText().toString());
            lngd = Double.parseDouble(lng.getText().toString());
        }catch(NumberFormatException e){
            exception = true;
        }
        if(exception){
            lat.setText(place.getLat()+"");
            lng.setText(place.getLng()+"");
            AddPlaceActivity.buildDialog(this, getString(R.string.error_latlng_title), getString(R.string.error_latlng_msg));
        }
        else{
            place.setNombre(name);
            place.setDesc(desc);
            place.setDireccion(address);
            place.setLat(latd);
            place.setLng(lngd);

            Intent intentResult = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(ShowPlaceActivity.EDIT_PLACE_OBJ, place);
            intentResult.putExtras(bundle);
            setResult(Activity.RESULT_OK, intentResult);
            finish();
        }
    }

    //Valido para seleccionar una opción del menu
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

}
