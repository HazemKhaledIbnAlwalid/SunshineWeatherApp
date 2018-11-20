package com.example.hazem.sunshineweatherapp.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.hazem.sunshineweatherapp.data.SunshinePreferences;
import com.example.hazem.sunshineweatherapp.data.WeatherContract;

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

    // variables for json parsing
    /* Location information */
    private static final String OWM_CITY = "city";
    private static final String OWM_COORD = "coord";

    /* Location coordinate */
    private static final String OWM_LATITUDE = "lat";
    private static final String OWM_LONGITUDE = "lon";

    /* Weather information. Each day's forecast info is an element of the "list" array */
    private static final String OWM_LIST = "list";

    private static final String OWM_PRESSURE = "pressure";
    private static final String OWM_HUMIDITY = "humidity";
    private static final String OWM_WINDSPEED = "speed";
    private static final String OWM_WIND_DIRECTION = "deg";

    /* All temperatures are children of the "temp" object */
    private static final String OWM_TEMPERATURE = "temp";

    /* Max temperature for the day */
    private static final String OWM_MAX = "max";
    private static final String OWM_MIN = "min";

    private static final String OWM_WEATHER = "weather";
    private static final String OWM_WEATHER_ID = "id";

    private static final String OWM_MESSAGE_CODE = "cod";

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
        /* String array to hold each day's weather String */
        String[] parsedWeatherData = null;

        try {
            JSONObject forecastJson = new JSONObject(serverResponse);

            /* Is there an error? */
            if (forecastJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);

                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        /* Location invalid */
                        return null;
                    default:
                        /* Server probably down */
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

                    int weatherId = -1;
                    String weatherDescription = "";
                    if (weatherObject.has(OWM_WEATHER_ID)) {
                        weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                        weatherDescription = WeatherUtils.getStringForWeatherCondition(
                                context,
                                weatherId);
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

    public static ContentValues[] getWeatherContentValuesFromJson(Context context, String forecastJsonStr) {

        ContentValues[] weatherContentValues = new ContentValues[numberOfDaysToPredict];

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);

            /* Is there an error? */
            if (forecastJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);

                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        /* Location invalid */
                        return null;
                    default:
                        /* Server probably down */
                        return null;
                }
            }

            if (forecastJson.has(OWM_LIST)) {
                JSONArray jsonWeatherArray = forecastJson.getJSONArray(OWM_LIST);

                if(forecastJson.has(OWM_CITY)) {
                    JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);


                    if(cityJson.has(OWM_COORD)) {
                        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);

                        double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
                        double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

                    SunshinePreferences.setLocationDetails(context, cityLatitude, cityLongitude);

                    }
                }

                // create those helper strings that will be used inside the loop to get the date
                long localDate = System.currentTimeMillis();
                long utcDate = SunshineDateUtils.getUTCDateFromLocal(localDate);
                long normalizedUtcStartDay = SunshineDateUtils.normalizeDate(utcDate);

                for (int i = 0; i < numberOfDaysToPredict; i++) {
                    /* Get the JSON object representing the day */
                    JSONObject dayForecast = jsonWeatherArray.getJSONObject(i);


                    long dateTimeMillis = normalizedUtcStartDay + SunshineDateUtils.DAY_IN_MILLIS * i;

                    double pressure = dayForecast.getDouble(OWM_PRESSURE);
                    int humidity = dayForecast.getInt(OWM_HUMIDITY);
                    double windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                    double windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                    /*
                     * Description is in a child array called "weather", which is 1 element long.
                     * That element also contains a weather code.
                     */
                    JSONObject weatherObject =
                            dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);

                    int weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                    /*
                     * Temperatures are sent by Open Weather Map in a child object called "temp".
                     *
                     * Editor's Note: Try not to name variables "temp" when working with temperature.
                     * It confuses everybody. Temp could easily mean any number of things, including
                     * temperature, temporary variable, temporary folder, temporary employee, or many
                     * others, and is just a bad variable name.
                     */
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    ContentValues weatherValues = new ContentValues();
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTimeMillis);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                    weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                    weatherContentValues[i] = weatherValues;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "getWeatherContentValuesFromJson Function: " + e.getMessage());
        }

        return weatherContentValues;
    }
}
