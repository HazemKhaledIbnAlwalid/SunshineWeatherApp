package com.example.hazem.sunshineweatherapp.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.example.hazem.sunshineweatherapp.data.SunshinePreferences;
import com.example.hazem.sunshineweatherapp.data.WeatherContract;
import com.example.hazem.sunshineweatherapp.utilities.NetworkUtils;
import com.example.hazem.sunshineweatherapp.utilities.NotificationUtils;

import java.net.URL;

public class SunshineSyncTask {

    private static final String TAG = "SunshineSyncTask";
    /**
     * Performs the network request for updated weather, parses the JSON from that request, and
     * inserts the new weather information into our ContentProvider. Will notify the user that new
     * weather has been loaded if the user hasn't been notified of the weather within the last day
     * AND they haven't disabled notifications in the preferences screen.
     *
     * @param context Used to access utility methods and the ContentResolver
     */
    synchronized public static void syncWeatherData(Context context) {

        try {
            URL weatherRequestUrl = NetworkUtils.getUrl(context);

            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

            ContentValues[] weatherData =  NetworkUtils.getWeatherContentValuesFromJson(context,jsonWeatherResponse);

            /*
             * In cases where our JSON contained an error code, getWeatherContentValuesFromJson
             * would have returned null. We need to check for those cases here to prevent any
             * NullPointerExceptions being thrown. We also have no reason to insert fresh data if
             * there isn't any to insert.
             */
            if (weatherData != null && weatherData.length != 0) {
                /* Get a handle on the ContentResolver to delete and insert data */
                ContentResolver sunshineContentResolver = context.getContentResolver();

                //If we have valid results, delete the old data and insert the new
                /* Delete old weather data because we don't need to keep multiple days' data */
                sunshineContentResolver.delete(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null);

                /* Insert our new weather data into Sunshine's ContentProvider */
                sunshineContentResolver.bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        weatherData);

                // Check if notifications are enabled
                /*
                 * Finally, after we insert data into the ContentProvider, determine whether or not
                 * we should notify the user that the weather has been refreshed.
                 */
                boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

                /*
                 * If the last notification was shown was more than 1 day ago, we want to send
                 * another notification to the user that the weather has been updated. Remember,
                 * it's important that you shouldn't spam your users with notifications.
                 */
                long timeSinceLastNotification = SunshinePreferences
                        .getEllapsedTimeSinceLastNotification(context);

                boolean oneDayPassedSinceLastNotification = false;

//              Check if a day has passed since the last notification
                if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                    oneDayPassedSinceLastNotification = true;
                }

                /*
                 * We only want to show the notification if the user wants them shown and we
                 * haven't shown a notification in the past day.
                 */
//              If more than a day have passed and notifications are enabled, notify the user
                if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                    NotificationUtils.notifyUserOfNewWeather(context);
                }

            }

            /* If the code reaches this point, we have successfully performed our sync */
        } catch (Exception e) {
            Log.e(TAG, "syncWeatherData Function: " +e.getMessage());
        }

    }
}
