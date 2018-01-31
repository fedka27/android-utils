package com.github.fedka27.app;

import android.content.Context;
import android.content.Intent;

import com.github.fedka27.download.service.DownloadItem;


public class TestDownload extends DownloadItem {
    @Override
    public String getFilename() {
        return "test.zip";
    }

    @Override
    public <T extends DownloadItem> Intent getIntent(Context context) {
        return new Intent();
    }
}
