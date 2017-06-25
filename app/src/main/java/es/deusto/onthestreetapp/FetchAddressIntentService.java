package es.deusto.onthestreetapp;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.internal.zzs.TAG;

/**
 * Created by kevin on 26/3/17.
 */

//Service to get the address of a lat/lng
public class FetchAddressIntentService extends IntentService{

    protected ResultReceiver mReceiver;

    public FetchAddressIntentService() {
        super("FetchAddress");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Double lat = intent.getDoubleExtra("lat", 0.0);
        Double lng = intent.getDoubleExtra("lng", 0.0);
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        if(lat != 0.0 && lng != 0.0){
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(lat, lng, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Handle case where no address was found.
            if (addresses == null || addresses.size()  == 0) {
                    Toast.makeText(FetchAddressIntentService.this, R.string.msg_error_server, Toast.LENGTH_SHORT).show();
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }

                deliverResultToReceiver(Constants.SUCCESS_RESULT,
                        TextUtils.join(System.getProperty("line.separator"),
                                addressFragments));
            }
        }
}
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

}

