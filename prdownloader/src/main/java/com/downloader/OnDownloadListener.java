package com.downloader;

/**
 * Created by amitshekhar on 13/11/17.
 */

public interface OnDownloadListener {

    void onDownloadComplete(String path);

    void onError(Error error);

}
