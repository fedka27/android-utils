package com.github.fedka27.download.service;

public interface DownloadServiceListener<T> {
    void onStarted(T item);

    void onProgress(T item, DownloadProgress download);

    void onComplete(T item);

    void onError(T item, Throwable throwable);
}
