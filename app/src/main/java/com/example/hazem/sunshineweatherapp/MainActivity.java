package com.example.hazem.sunshineweatherapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.hazem.sunshineweatherapp.RecyclerViewPack.ForecastAdapter;
import com.example.hazem.sunshineweatherapp.data.SunshinePreferences;
import com.example.hazem.sunshineweatherapp.databinding.ActivityMainBinding;
import com.example.hazem.sunshineweatherapp.utilities.NetworkUtils;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        ForecastAdapter.ForecastAdapterOnClickHandler,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<String[]> {


    private static final int FORECAST_LOADER_ID = 0;

    private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;

    private ActivityMainBinding mainBinding;

    private ForecastAdapter mForecastAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // prepare and start the recyclerView
        int recyclerViewOrientation = LinearLayoutManager.VERTICAL;

        boolean shouldReverseLayout = false;

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getApplicationContext(),recyclerViewOrientation,shouldReverseLayout);

        mainBinding.rvWeatherInfo.setLayoutManager(layoutManager);

        mainBinding.rvWeatherInfo.setHasFixedSize(true);

        mForecastAdapter = new ForecastAdapter(this);

        mainBinding.rvWeatherInfo.setAdapter(mForecastAdapter);

        //start Loader Manager
        getSupportLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);

        //register shared preference to read from it
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
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
        if (data != null) {
            mForecastAdapter.setWeatherData(data);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String[]> loader) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int clickedItemId = item.getItemId();

        switch (clickedItemId){
            case R.id.action_map_location:
                openMapAndViewUserLocation();

                return true;

            case R.id.action_settings:
                Intent settingsIntent =
                        new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(settingsIntent);

                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    private void openMapAndViewUserLocation(){
        String addressString = SunshinePreferences.getPreferredWeatherLocation(getApplicationContext());

        Uri addressUri = Uri.parse("geo:0,0?q=" + addressString);

        Intent openMapIntent = new Intent(Intent.ACTION_VIEW);

        openMapIntent.setData(addressUri);

        if(openMapIntent.resolveActivity(getPackageManager())!=null) {
            startActivity(openMapIntent);
        }
        else {
            Toast.makeText(getApplicationContext(),getString(R.string.no_map_app_on_your_device),Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onClick(String weatherForDay) {
        /*Context context = this;
        Class destinationClass = DetailActivity.class;
        Intent intentToStartDetailActivity = new Intent(context, destinationClass);
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT, weatherForDay);
        startActivity(intentToStartDetailActivity);*/
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PREFERENCES_HAVE_BEEN_UPDATED = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (PREFERENCES_HAVE_BEEN_UPDATED){
            getSupportLoaderManager()
                    .restartLoader(FORECAST_LOADER_ID,null,this);

            PREFERENCES_HAVE_BEEN_UPDATED = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
