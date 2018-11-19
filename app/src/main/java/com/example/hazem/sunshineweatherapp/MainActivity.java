package com.example.hazem.sunshineweatherapp;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.example.hazem.sunshineweatherapp.data.SunshinePreferences;
import com.example.hazem.sunshineweatherapp.databinding.ActivityMainBinding;
import com.example.hazem.sunshineweatherapp.utilities.NetworkUtils;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<String[]> {


    private static final int FORECAST_LOADER_ID = 0;
    private ActivityMainBinding mainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        getSupportLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
    }

    @NonNull
    @Override
    public Loader<String[]> onCreateLoader(int id, @Nullable final Bundle args) {
        return new AsyncTaskLoader<String[]>(this) {

            /* This String array will hold and help cache our weather data */
            String[] mWeatherData = null;

            @Override
            protected void onStartLoading() {
                if (mWeatherData != null) {
                    deliverResult(mWeatherData);
                } else {
                    forceLoad();
                }
            }

            @Nullable
            @Override
            public String[] loadInBackground() {
                String userLocation = SunshinePreferences.getPreferredWeatherLocation(getApplicationContext());
                URL requestUrl = NetworkUtils.buildUrl(userLocation);
                String weatherServerResponse = NetworkUtils.getResponseFromHttpUrl(requestUrl);

                String[] parsedWeatherData = NetworkUtils.getSimpleWeatherDataFromJson(getApplicationContext(), weatherServerResponse);
                return parsedWeatherData;
            }

            /**
             * Sends the result of the load to the registered listener.
             *
             * @param data The result of the load
             */
            public void deliverResult(String[] data) {
                mWeatherData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String[]> loader, String[] data) {
        String weatherDataString = "";
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                weatherDataString += (data[i] + "\n\n");
            }
            mainBinding.tvWeatherInfo.setText(weatherDataString);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String[]> loader) {

    }
}
