package com.sample;

import android.app.Application;
import android.util.Log;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;

/**
 * Created by amitshekhar on 13/11/17.
 */

public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                    .setDatabaseEnabled(true)
                    .build();
            PRDownloader.initialize(this, config);
        }catch (Exception e){
            Log.i("Download", "onCreate: e" + e.getMessage());
        }
    }

}
