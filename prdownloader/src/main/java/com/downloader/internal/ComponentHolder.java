package com.downloader.internal;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.downloader.Constants;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.downloader.database.AppDbHelper;
import com.downloader.database.DbHelper;
import com.downloader.database.NoOpsDbHelper;
import com.downloader.httpclient.DefaultHttpClient;
import com.downloader.httpclient.HttpClient;
import com.downloader.utils.ScopedStorageHelper;
import com.downloader.utils.Utils;

import java.io.File;

/**
 * Created by amitshekhar on 14/11/17.
 */

public class ComponentHolder {

    private final static ComponentHolder INSTANCE = new ComponentHolder();
    private int readTimeout;
    private int connectTimeout;
    private String userAgent;
    private HttpClient httpClient;
    private DbHelper dbHelper;
    private String tempFilePath;

    private String downloadDirName;
    private Context context;

    public static ComponentHolder getInstance() {
        return INSTANCE;
    }

    public void init(Context context, PRDownloaderConfig config) {
        this.readTimeout = config.getReadTimeout();
        this.connectTimeout = config.getConnectTimeout();
        this.userAgent = config.getUserAgent();
        this.httpClient = config.getHttpClient();
        this.context = context;
        this.dbHelper = config.isDatabaseEnabled() ? new AppDbHelper(context) : new NoOpsDbHelper();
        if (config.isDatabaseEnabled()) {
            PRDownloader.cleanUp(30);
        }
        this.tempFilePath = config.getTempPath();
        this.downloadDirName = config.getDownloadDirName();
        if ((config.getTempPath() == null || config.getTempPath().isEmpty()) && Utils.isExternalStorageAvailable() && Utils.isExternalStorageReadWrite()) {
            File externalDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (externalDownloadDir != null) {
                this.tempFilePath = externalDownloadDir.getPath();
            } else {
                this.tempFilePath = context.getCacheDir().getPath();
            }
        }
        Log.d("Download temp path", this.tempFilePath);
    }

    public int getReadTimeout() {
        if (readTimeout == 0) {
            synchronized (ComponentHolder.class) {
                if (readTimeout == 0) {
                    readTimeout = Constants.DEFAULT_READ_TIMEOUT_IN_MILLS;
                }
            }
        }
        return readTimeout;
    }

    public int getConnectTimeout() {
        if (connectTimeout == 0) {
            synchronized (ComponentHolder.class) {
                if (connectTimeout == 0) {
                    connectTimeout = Constants.DEFAULT_CONNECT_TIMEOUT_IN_MILLS;
                }
            }
        }
        return connectTimeout;
    }

    public String getUserAgent() {
        if (userAgent == null) {
            synchronized (ComponentHolder.class) {
                if (userAgent == null) {
                    userAgent = Constants.DEFAULT_USER_AGENT;
                }
            }
        }
        return userAgent;
    }

    public DbHelper getDbHelper() {
        if (dbHelper == null) {
            synchronized (ComponentHolder.class) {
                if (dbHelper == null) {
                    dbHelper = new NoOpsDbHelper();
                }
            }
        }
        return dbHelper;
    }

    public HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (ComponentHolder.class) {
                if (httpClient == null) {
                    httpClient = new DefaultHttpClient();
                }
            }
        }
        return httpClient.clone();
    }

    public String getTempFilePath() {
        if (tempFilePath == null) {
            synchronized (ComponentHolder.class) {
                if (tempFilePath == null) {
                    tempFilePath = "";
                }
            }
        }
        return tempFilePath;
    }

    public Context getContext() {
        return context;
    }

    public String getDefaultDownloadFilePath(String param) {
        String downloadFilePath = param;
        if (param == null || param.isEmpty()) {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloadDir != null) {
                downloadFilePath = downloadDir.getPath();
            }
        }
        if (downloadFilePath != null && !downloadFilePath.isEmpty() && this.downloadDirName != null && !this.downloadDirName.isEmpty()) {
            downloadFilePath = downloadFilePath + File.separator + this.downloadDirName;
        }
        if (!ScopedStorageHelper.validateInputPath(this.getContext(), downloadFilePath)) {
            throw new UnsupportedOperationException("download path error! downloadPath : " + downloadFilePath);
        }
        Log.d("Download path", downloadFilePath);
        return downloadFilePath;
    }

}
