package com.example.hazem.sunshineweatherapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.example.hazem.sunshineweatherapp.data.SunshinePreferences;
import com.example.hazem.sunshineweatherapp.data.WeatherContract;
import com.example.hazem.sunshineweatherapp.sync.SunshineSyncUtils;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference_general_settings);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();

        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference preference = preferenceScreen.getPreference(i);

            if(!(preference instanceof CheckBoxPreference)){
                String value = sharedPreferences.getString(preference.getKey(),"");
                setPreferenceSummary(preference,value);
            }
        }
    }

    private void setPreferenceSummary(Preference preference,String value){

        if(preference instanceof ListPreference){
            ListPreference listPreference = (ListPreference)preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if(prefIndex >= 0){
                // set the summary with the proper value
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }
        else {
            // otherwise
            preference.setSummary(value);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity activity = getActivity();

        if (key.equals(getString(R.string.pref_location_key))) {
            // we've changed the location
            // Wipe out any potential PlacePicker latlng values so that we can use this text entry.
            SunshinePreferences.resetLocationCoordinates(activity);

            // Sync the weather if the location changes
            SunshineSyncUtils.startImmediateSync(activity);
        }
        else if (key.equals(getString(R.string.pref_units_key))) {
            // units have changed. update lists of weather entries accordingly
            activity.getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }

        Preference preference = findPreference(key);
        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {
                setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
