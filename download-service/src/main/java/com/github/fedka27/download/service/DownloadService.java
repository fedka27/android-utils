package com.github.fedka27.download.service;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.StringDef;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class DownloadService<T extends DownloadItem> extends IntentService {
    public static final String ACTION_STARTED = "DownloadService.STARTED";
    public static final String ACTION_PROGRESS = "DownloadService.PROCESSING";
    public static final String ACTION_COMPLETE = "DownloadService.COMPLETE";
    public static final String ACTION_CANCEL = "DownloadService.COMPLETE";
    public static final String ACTION_ERROR = "DownloadService.ERROR";
    private static final String TAG = DownloadService.class.getSimpleName();
    private static final String PERMISSION_STORAGE_READ = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String PERMISSION_STORAGE_WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String[] PERMISSIONS_STORAGE = new String[]{PERMISSION_STORAGE_READ, PERMISSION_STORAGE_WRITE};
    private static final int PERMISSION_REQUEST_CODE = 765;

    private static final String EXTRA_DATA = TAG + "_DATA";
    private static final String EXTRA_DOWNLOAD = TAG + "_DOWNLOAD";
    private static final String EXTRA_ERROR = TAG + "_ERROR";

    private static final String EXTRA_NOTIFICATION_ID = TAG + "notification_id";

    private Set<DownloadServiceListener<T>> downloaditemListeners = new HashSet<>();

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (intent.getAction() == null) return;

            T item = (T) intent.getSerializableExtra(EXTRA_DATA);

            if (item == null) return;

            if (intent.getAction().equals(ACTION_STARTED)) {
                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onStarted(item);
                }
                return;
            }

            if (intent.getAction().equals(ACTION_PROGRESS)) {
                DownloadProgress download = (DownloadProgress) intent.getSerializableExtra(EXTRA_DOWNLOAD);
                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onProgress(item, download);
                }
                return;
            }

            if (intent.getAction().equals(ACTION_COMPLETE)) {
                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onComplete(item);
                }
                return;
            }

            if (intent.getAction().equals(ACTION_ERROR)) {
                Throwable throwable = (Throwable) intent.getSerializableExtra(EXTRA_ERROR);

                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onError(item, throwable);
                }
                return;
            }

            if (intent.getAction().equals(ACTION_CANCEL)) {
                Log.d(TAG, "Canceled");
                stopSelf();
                notificationManager.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1));

                File file = new File(item.getFilepath());
                if (file.exists()) {
                    file.delete();
                }
                return;
            }
        }
    };

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     * <p>
     * Used to name the worker thread, important only for debugging.
     */
    public DownloadService() {
        super(TAG);
        setIntentRedelivery(true);
    }

    public void startDownloading(Activity activity, T item) {

        if (ContextCompat.checkSelfPermission(activity, PERMISSION_STORAGE_READ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity, PERMISSION_STORAGE_WRITE) == PackageManager.PERMISSION_GRANTED) {

            startPermissionGranted(activity, item);
        } else {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, PERMISSION_REQUEST_CODE);
        }

    }

    private void startPermissionGranted(Activity activity, T item) {
        Intent intent = new Intent(activity, DownloadService.class);
        intent.putExtra(EXTRA_DATA, item);
        activity.startService(intent);
    }

    public void onRequestPermissionResult(Activity activity, T item, int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                startPermissionGranted(activity, item);
            }
        }
    }

    public void registerListener(Context context, DownloadServiceListener<T> listener) {
        downloaditemListeners.add(listener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_STARTED);
        intentFilter.addAction(ACTION_PROGRESS);
        intentFilter.addAction(ACTION_COMPLETE);
        intentFilter.addAction(ACTION_ERROR);
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void unregister(DownloadServiceListener<T> downloaditemListener) {
        downloaditemListeners.remove(downloaditemListener);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        T item = (T) intent.getSerializableExtra(EXTRA_DATA);

        int ID = item.getNotificationId();

        PendingIntent pendingIntentCancel = PendingIntent.getService(this,
                0,
                new Intent(ACTION_CANCEL)
                        .putExtra(EXTRA_NOTIFICATION_ID, ID)
                        .putExtra(EXTRA_DATA, item),
                PendingIntent.FLAG_CANCEL_CURRENT);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setSubText(item.getFilename())
                .addAction(R.drawable.ic_close, getString(R.string.notification_cancel), pendingIntentCancel)
                .setOngoing(true)
                .setAutoCancel(false);
        notificationManager.notify(ID, notificationBuilder.build());

        item.setDownloading(true);
        sendActionIntent(ACTION_STARTED, item);

        try {
            initDownload(item);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: 31.01.18 error notification;

            item.setDownloading(false);

            notificationManager.cancel(item.getNotificationId());

            sendActionIntentError(item, e);
        }

    }

    private void initDownload(T item) throws IOException {

        URL url = new URL(item.getDownloadUrl());
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {

            item.setDownloading(true);

            int contentLength = httpConn.getContentLength();

            int count;
            byte data[] = new byte[1024 * 4];

            InputStream bis = new BufferedInputStream(httpConn.getInputStream(), 1024 * 8);
            File outputFile = new File(item.getFilepath());
            OutputStream output = new FileOutputStream(outputFile);

            long total = 0;
            long startTime = System.currentTimeMillis();
            int timeCount = 1;

            while ((count = bis.read(data)) != -1) {

                total += count;
                int totalFileSize = (int) ((long) contentLength / (Math.pow(1024, 2)));
                double current = Math.round(total / (Math.pow(1024, 2)));

                int progress = (int) ((total * 100) / (long) contentLength);
                long currentTime = System.currentTimeMillis() - startTime;

                DownloadProgress download = new DownloadProgress();
                download.setTotalFileSize(totalFileSize);

                if (currentTime > 1000 * timeCount) {

                    item.setDownloading(true);
                    download.setCurrentFileSize((int) current);
                    download.setProgress(progress);
                    timeCount++;

                    sendProcessingNotification(item, download);
                }

                output.write(data, 0, count);
            }
            onDownloadComplete(item);
            output.flush();
            output.close();
            bis.close();

            System.out.println("File downloaded");
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

    private void sendProcessingNotification(T item, DownloadProgress download) {
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.putExtra(EXTRA_DATA, item);
        intent.putExtra(EXTRA_DOWNLOAD, download);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        notificationBuilder.setProgress(100, download.getProgress(), false);
        notificationBuilder.setContentText(getString(R.string.notification_progress, download.getProgress()));
        notificationManager.notify(item.getNotificationId(), notificationBuilder.build());
    }

    private void sendActionIntent(@Actions String action, T item) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, item);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendActionIntentError(T item, Throwable throwable) {
        Intent intent = new Intent(ACTION_ERROR);
        intent.putExtra(EXTRA_DATA, item);
        intent.putExtra(EXTRA_ERROR, throwable);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onDownloadComplete(T item) {

        int id = item.getNotificationId();

        item.setDownloading(false);

        sendActionIntent(ACTION_COMPLETE, item);

        notificationManager.cancel(id);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("File Downloaded");
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        notificationBuilder.setOngoing(false);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setContentIntent(PendingIntent
                .getActivity(this, 0, item.getIntent(this), 0));
        notificationManager.notify(id, notificationBuilder.build());

    }

    @StringDef({ACTION_STARTED, ACTION_PROGRESS, ACTION_COMPLETE, ACTION_ERROR})
    private @interface Actions {
    }
}
