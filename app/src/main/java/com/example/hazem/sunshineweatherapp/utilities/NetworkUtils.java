package com.example.hazem.sunshineweatherapp.utilities;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class NetworkUtils {

    // this TAG contains class name which is used in log
    private static final String TAG = NetworkUtils.class.getSimpleName();

    // The Url of the weather server (openWeather.com sever is used by udacity)
    // which the data come from (the server provides the weather data for the next 14 days)
    private static final String WEATHER_API_BASE_URL = "https://andfun-weather.udacity.com/weather";

    //the query parameters of the url
    //the main query parameter here is the location q=location
    private static final String QUERY_PARAM = "q";
    private static final String LAT_PARAM = "lat";
    private static final String LON_PARAM = "lon";
    private static final String FORMAT_PARAM = "mode";
    private static final String UNITS_PARAM = "units";

    //the query values assigned to the parameters in tht url
    private static final String dataFormat = "json";
    // metric = temp will be in Celsius
    private static final String unit = "metric";
    //this variable determine how many days in the future app will provide
    // weather information about it
    private static final Integer numberOfDaysToPredict = 5;

    /*
     * this method builds the final url used to query the weather server
     *
     *   @param userLocation the main query parameter
     *
     *   @return completedUrl which is used to query the weather server
     */
    public static URL buildUrl(String userLocation) {
        Uri builtUri = Uri.parse(WEATHER_API_BASE_URL).buildUpon()
                .appendQueryParameter(QUERY_PARAM, userLocation)
                .appendQueryParameter(UNITS_PARAM, unit)
                .appendQueryParameter(FORMAT_PARAM, dataFormat)
                .build();

        URL completedUrl = null;
        try {
            completedUrl = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            Log.e(TAG, "buildUrl Function: " + e.getMessage());
        }
        return completedUrl;
    }

    /*
     * this method is receives the response of the weather sever and return it
     *
     * @param weatherServerUrl which is used to query the weather server
     *
     * @return serverResponse which is the json format response
     */

    public static String getResponseFromHttpUrl(URL weatherServerUrl) {

        String serverResponse = "";
        HttpURLConnection mConnection = null;
        try {
            mConnection = (HttpURLConnection) weatherServerUrl.openConnection();

            InputStream mInputStream = mConnection.getInputStream();

            // use the scanner to read until the end of file
            Scanner mScanner = new Scanner(mInputStream);
            mScanner.useDelimiter("\\A");

            boolean hasInput = mScanner.hasNext();

            if (hasInput) {
                serverResponse = mScanner.next();
            }
        } catch (IOException e) {
            Log.e(TAG, "getResponseFromHttpUrl Function: " + e.getMessage());
        } finally {
            mConnection.disconnect();
        }
        return serverResponse;
    }

    public static String[] getSimpleWeatherDataFromJson(Context context, String serverResponse) {
        /* Location information */
        String OWM_CITY = "city";
        String OWM_COORD = "coord";

        /* Location coordinate */
        String OWM_LATITUDE = "lat";
        String OWM_LONGITUDE = "lon";

        /* Weather information. Each day's forecast info is an element of the "list" array */
        String OWM_LIST = "list";

        String OWM_PRESSURE = "pressure";
        String OWM_HUMIDITY = "humidity";
        String OWM_WINDSPEED = "speed";
        String OWM_WIND_DIRECTION = "deg";

        /* All temperatures are children of the "temp" object */
        String OWM_TEMPERATURE = "temp";

        /* Max temperature for the day */
        String OWM_MAX = "max";
        String OWM_MIN = "min";

        String OWM_WEATHER = "weather";
        String OWM_DESCRIPTION = "description";
        String OWM_WEATHER_ID = "id";

        String OWM_MESSAGE_CODE = "cod";

        /* String array to hold each day's weather String */
        String[] parsedWeatherData = null;

        try {
            JSONObject forecastJson = new JSONObject(serverResponse);

            if (forecastJson.has(OWM_MESSAGE_CODE)) {
                int serverResponseCode = forecastJson.getInt(OWM_MESSAGE_CODE);
                switch (serverResponseCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        return null;
                    default:
                        return null;
                }
            }

            if (forecastJson.has(OWM_LIST)) {
                // create those helper strings that will be used inside the loop to get the date
                long localDate = System.currentTimeMillis();
                long utcDate = SunshineDateUtils.getUTCDateFromLocal(localDate);
                long startDay = SunshineDateUtils.normalizeDate(utcDate);

                // get the list of weather data for the next numberOfDays (which is = 7)
                JSONArray forecastArray = forecastJson.getJSONArray(OWM_LIST);

                parsedWeatherData = new String[numberOfDaysToPredict];

                for (int i = 0; i < numberOfDaysToPredict; i++) {
                    // get single day weather data
                    JSONObject dayForecast = forecastArray.getJSONObject(i);

                    //get weather description
                    //start
                    JSONArray weatherArray = new JSONArray();
                    if (dayForecast.has(OWM_WEATHER)) {
                        weatherArray = dayForecast.getJSONArray(OWM_WEATHER);
                    }

                    JSONObject weatherObject = new JSONObject();
                    if (weatherArray.length() > 0) {
                        weatherObject = weatherArray.getJSONObject(0);
                    }

                    String weatherDescription = "";
                    if (weatherObject.has(OWM_DESCRIPTION)) {
                        weatherDescription = weatherObject.getString(OWM_DESCRIPTION);
                    }
                    //End

                    //get the Temperature from json
                    //start
                    Double maxTemp = new Double(0.0);
                    Double minTemp = new Double(0.0);

                    JSONObject weatherTemperatureObject = new JSONObject();
                    if (dayForecast.has(OWM_TEMPERATURE)) {
                        weatherTemperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);

                        if (weatherTemperatureObject.has(OWM_MAX)) {
                            maxTemp = weatherTemperatureObject.getDouble(OWM_MAX);
                        }

                        if (weatherTemperatureObject.has(OWM_MIN)) {
                            minTemp = weatherTemperatureObject.getDouble(OWM_MIN);
                        }
                    }
                    //End

                    // create the date string
                    long dateTimeMillis = startDay + SunshineDateUtils.DAY_IN_MILLIS * i;
                    String friendlyFormattedDate = SunshineDateUtils.getFriendlyDateString(
                            context,
                            dateTimeMillis,
                            false);
                    //End
                    parsedWeatherData[i] = friendlyFormattedDate + "-"
                            + weatherDescription + "-"
                            + WeatherUtils.formatHighLows(context, maxTemp, minTemp);
                }

            }
        } catch (JSONException e) {
            Log.e(TAG, "getSimpleWeatherDataFromJson Function: " + e.getMessage());
        }
        return parsedWeatherData;
    }
}
