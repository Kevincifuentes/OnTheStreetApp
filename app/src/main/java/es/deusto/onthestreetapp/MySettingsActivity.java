package es.deusto.onthestreetapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.RelativeLayout;

public class MySettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MySettingsFragment())
                .commit();
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.settings);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            Intent intentResult = new Intent();
            setResult(Activity.RESULT_OK, intentResult);
            finish();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void startCheckService() {
        Intent i = new Intent(getApplicationContext(), CheckNearestPlaceService.class);
        getApplicationContext().startService(i);
    }

    public void stopCheckService() {
        Intent i = new Intent(getApplicationContext(), CheckNearestPlaceService.class);
        getApplicationContext().stopService(i);
    }
}
