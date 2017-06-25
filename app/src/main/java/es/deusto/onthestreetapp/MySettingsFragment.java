package es.deusto.onthestreetapp;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import static es.deusto.onthestreetapp.PlaceWidget.WIDGET_COLOR_KEY;
import static es.deusto.onthestreetapp.PlaceWidget.WIDGET_DATA_KEY;
import static es.deusto.onthestreetapp.PlaceWidget.WIDGET_IDS_KEY;

public class MySettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private Activity activity;
    private String color;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        // Register for changes in preferences
        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        initSummary(getPreferenceScreen());

        activity = getActivity();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        color = sharedPrefs.getString("widget_preference", getResources().getStringArray(R.array.colores)[0]);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		// In the case of change in username, replace the default summary with the new value
		if(key.equals("username")){
            this.findPreference(key).setSummary(sharedPreferences.getString(key, ""));
            sharedPreferences.edit().putString(key, sharedPreferences.getString(key, "")).apply();
        }
        else if (key.equals("locationService")){
            if (sharedPreferences.getBoolean(key, false)) {
                Log.i("CheckService", "Yes");
                ((MySettingsActivity)activity).startCheckService();

            } else {
                Log.i("CheckService", "No");
                ((MySettingsActivity)activity).stopCheckService();
            }
            sharedPreferences.edit().putBoolean(key, sharedPreferences.getBoolean(key, false)).apply();
        }
        else if (key.equals("addCurrent")){
            sharedPreferences.edit().putBoolean(key, sharedPreferences.getBoolean(key, false)).apply();
        }
        else if (key.equals("radiusPlace")){
            this.findPreference(key).setSummary(sharedPreferences.getString(key, "100"));
            sharedPreferences.edit().putString(key, sharedPreferences.getString(key, "100")).apply();
        }
        else if(key.equals("widget_preference")){
            this.findPreference(key).setSummary(sharedPreferences.getString(key, activity.getResources().getStringArray(R.array.colores)[0]));
            sharedPreferences.edit().putString(key, sharedPreferences.getString(key, activity.getResources().getStringArray(R.array.colores)[0])).apply();
            if(!(sharedPreferences.getString(key, activity.getResources().getStringArray(R.array.colores)[0]).equals(color))){
                color = sharedPreferences.getString(key, activity.getResources().getStringArray(R.array.colores)[0]);
                //BROADCAST
                Intent intent = new Intent(activity.getApplicationContext(), PlaceWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
                // since it seems the onUpdate() is only fired on that:
                AppWidgetManager widgetManager = AppWidgetManager.getInstance(activity.getApplicationContext());
                int[] ids = widgetManager.getAppWidgetIds(new ComponentName(activity.getApplicationContext(), PlaceWidget.class));

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

                intent.putExtra(WIDGET_IDS_KEY, ids);
                intent.putExtra(WIDGET_COLOR_KEY, color);
                activity.getApplicationContext().sendBroadcast(intent);
            }
        }
	}

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().toLowerCase().contains("password"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }
	
}
