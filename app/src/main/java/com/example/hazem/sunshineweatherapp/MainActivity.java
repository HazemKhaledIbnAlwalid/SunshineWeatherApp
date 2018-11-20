package com.example.hazem.sunshineweatherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.hazem.sunshineweatherapp.RecyclerViewPack.ForecastAdapter;
import com.example.hazem.sunshineweatherapp.data.SunshinePreferences;
import com.example.hazem.sunshineweatherapp.data.WeatherContract;
import com.example.hazem.sunshineweatherapp.databinding.ActivityMainBinding;
import com.example.hazem.sunshineweatherapp.utilities.FakeDataUtils;
import com.example.hazem.sunshineweatherapp.utilities.NetworkUtils;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        ForecastAdapter.ForecastAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MainActivity";
    
    private static final int FORECAST_LOADER_ID = 0;

    private int mPosition = RecyclerView.NO_POSITION;

    private ActivityMainBinding mainBinding;

    private ForecastAdapter mForecastAdapter;

    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
    };

    public static final int INDEX_WEATHER_DATE = 0;
    public static final int INDEX_WEATHER_MAX_TEMP = 1;
    public static final int INDEX_WEATHER_MIN_TEMP = 2;
    public static final int INDEX_WEATHER_CONDITION_ID = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setElevation(0f);

        // just for testing purposes
        FakeDataUtils.insertFakeData(this);

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // prepare and start the recyclerView
        int recyclerViewOrientation = LinearLayoutManager.VERTICAL;

        boolean shouldReverseLayout = false;

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getApplicationContext(),recyclerViewOrientation,shouldReverseLayout);

        mainBinding.rvWeatherInfo.setLayoutManager(layoutManager);

        mainBinding.rvWeatherInfo.setHasFixedSize(true);

        mForecastAdapter = new ForecastAdapter(this,this);

        mainBinding.rvWeatherInfo.setAdapter(mForecastAdapter);

        showLoading();

        //start Loader Manager
        getSupportLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case FORECAST_LOADER_ID:
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
                /*
                 * A SELECTION in SQL declares which rows you'd like to return. In our case, we
                 * want all weather data from today onwards that is stored in our weather table.
                 * We created a handy method to do that in our WeatherEntry class.
                 */
                String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();

                return new CursorLoader(this,
                        forecastQueryUri,
                        MAIN_FORECAST_PROJECTION,
                        selection,
                        null,
                        sortOrder);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

         mForecastAdapter.swapCursor(data);

        if (mPosition == RecyclerView.NO_POSITION){
            mPosition = 0;
        }

        mainBinding.rvWeatherInfo.smoothScrollToPosition(mPosition);

        if (data.getCount() != 0){
            showWeatherDataView();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    private void showWeatherDataView() {
        /* First, hide the loading indicator */
        mainBinding.pbLoadingIndicator.setVisibility(View.INVISIBLE);
        /* Finally, make sure the weather data is visible */
        mainBinding.rvWeatherInfo.setVisibility(View.VISIBLE);
    }


    private void showLoading() {
        /* Then, hide the weather data */
        mainBinding.rvWeatherInfo.setVisibility(View.INVISIBLE);
        /* Finally, show the loading indicator */
        mainBinding.pbLoadingIndicator.setVisibility(View.VISIBLE);
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
        double[] coords = SunshinePreferences.getLocationCoordinates(this);
        String posLat = Double.toString(coords[0]);
        String posLong = Double.toString(coords[1]);
        Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
        }
    }

    @Override
    public void onClick(long date) {
        Intent intentToStartDetailsActivity = new Intent(
                MainActivity.this,
                DetailsActivity.class);

        Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(date);
        intentToStartDetailsActivity.setData(uriForDateClicked);

        startActivity(intentToStartDetailsActivity);
    }
}
