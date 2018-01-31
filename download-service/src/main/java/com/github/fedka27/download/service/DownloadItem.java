package com.github.fedka27.download.service;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.Serializable;

public abstract class DownloadItem implements Serializable {

    private boolean isDownloading = false;
    private String downloadUrl;

    public abstract String getFilename();

    public abstract <T extends DownloadItem> Intent getIntent(Context context);

    /*Getters*/

    public int getNotificationId() {
        return getFilename().hashCode();
    }

    public boolean isDownloaded() {
        return new File(getFilepath()).exists() && !isDownloading();
    }

    final public boolean isDownloading() {
        return isDownloading;
    }

    final protected void setDownloading(boolean downloading) {
        isDownloading = downloading;
    }

    /*Setters*/

    final public String getFilepath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +
                File.separator + getFilename();
    }

    final public String getDownloadUrl() {
        return downloadUrl;
    }

    final public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
