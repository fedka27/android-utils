package com.github.fedka27.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.github.fedka27.download.service.DownloadProgress;
import com.github.fedka27.download.service.DownloadService;
import com.github.fedka27.download.service.DownloadServiceListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private TestDownload testDownload;
    private DownloadService<TestDownload> downloadService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String url = "https://github.com/fedka27/road-rules-content/raw/master/Appodeal-Android-SDK-2.1.7-251017-all-nodex.zip";

        testDownload = new TestDownload();
        testDownload.setDownloadUrl(url);

        downloadService = new DownloadService<>();

        downloadService.registerListener(this, new DownloadServiceListener<TestDownload>() {
            @Override
            public void onStarted(TestDownload testDownload) {
                Log.d(TAG, "Started - " + testDownload.getFilename());
            }

            @Override
            public void onProgress(TestDownload testDownload, DownloadProgress downloadProgress) {
                Log.d(TAG, "Progress - " + downloadProgress.getProgress());
            }

            @Override
            public void onComplete(TestDownload testDownload) {
                Log.d(TAG, "Complete - " + testDownload.getFilename());
            }

            @Override
            public void onError(TestDownload testDownload, Throwable throwable) {
                Log.d(TAG, "Error - " + testDownload.getFilename() + "\n" + throwable.getMessage());
            }
        });
    }


    public void downloadTestFile(View view) {
        downloadService.startDownloading(this, testDownload);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        downloadService.onRequestPermissionResult(this, testDownload, requestCode, permissions, grantResults);
    }
}
