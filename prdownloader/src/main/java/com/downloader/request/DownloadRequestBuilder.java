package com.downloader.request;

import com.downloader.Priority;
import com.downloader.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by amitshekhar on 13/11/17.
 */

public class DownloadRequestBuilder implements RequestBuilder {

    int downloadId;
    String url;
    String dirPath;
    String fileName;
    Priority priority = Priority.MEDIUM;
    Object tag;
    int readTimeout;
    int connectTimeout;
    int sendTimeLimit;
    String userAgent;
    HashMap<String, List<String>> headerMap;

    public DownloadRequestBuilder(int downloadId,String url, String dirPath, String fileName) {
        this.downloadId = downloadId;
        this.url = url;
        this.dirPath = dirPath;
        this.fileName = fileName;
    }

    public DownloadRequestBuilder(String url, String dirPath, String fileName) {
        this.downloadId = Utils.getUniqueId(url,dirPath,fileName);
        this.url = url;
        this.dirPath = dirPath;
        this.fileName = fileName;
    }

    @Override
    public DownloadRequestBuilder setHeader(String name, String value) {
        if (headerMap == null) {
            headerMap = new HashMap<>();
        }
        List<String> list = headerMap.get(name);
        if (list == null) {
            list = new ArrayList<>();
            headerMap.put(name, list);
        }
        if (!list.contains(value)) {
            list.add(value);
        }
        return this;
    }

    @Override
    public DownloadRequestBuilder setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public DownloadRequestBuilder setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    @Override
    public DownloadRequestBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public DownloadRequestBuilder setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public DownloadRequestBuilder setSendTimeLimit(int sendTimeLimit) {
        this.sendTimeLimit = sendTimeLimit;
        return this;
    }

    @Override
    public DownloadRequestBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public DownloadRequest build() {
        return new DownloadRequest(this);
    }

}
