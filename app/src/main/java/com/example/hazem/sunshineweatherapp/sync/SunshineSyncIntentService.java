package com.example.hazem.sunshineweatherapp.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SunshineSyncIntentService extends IntentService {

    public SunshineSyncIntentService() {
        super("SunshineSyncIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SunshineSyncTask.syncWeatherData(this);
    }
}
