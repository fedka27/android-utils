package com.github.fedka27.download.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.StringDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

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
    public static final String ACTION_ERROR = "DownloadService.ERROR";
    private static final String TAG = DownloadService.class.getSimpleName();
    private static final String EXTRA_DATA = TAG + "_DATA";
    private static final String EXTRA_DOWNLOAD = TAG + "_DOWNLOAD";
    private static final String EXTRA_ERROR = TAG + "_ERROR";

    private Set<DownloadServiceListener<T>> downloaditemListeners = new HashSet<>();

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
            }

            if (intent.getAction().equals(ACTION_PROGRESS)) {
                DownloadProgress download = (DownloadProgress) intent.getSerializableExtra(EXTRA_DOWNLOAD);
                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onProgress(item, download);
                }
            }

            if (intent.getAction().equals(ACTION_COMPLETE)) {
                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onComplete(item);
                }
            }

            if (intent.getAction().equals(ACTION_ERROR)) {
                Throwable throwable = (Throwable) intent.getSerializableExtra(EXTRA_ERROR);

                for (DownloadServiceListener<T> itemListener : downloaditemListeners) {
                    itemListener.onError(item, throwable);
                }
            }
        }
    };

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int ID = 111;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     * <p>
     * Used to name the worker thread, important only for debugging.
     */
    public DownloadService() {
        super(TAG);
        setIntentRedelivery(true);
    }

    public void startService(Context context, T item) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(EXTRA_DATA, item);
        context.startService(intent);
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

        ID = item.getNotificationId();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.getFilename())
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

            notificationManager.cancel(ID);

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
        notificationBuilder.setSubText(getString(R.string.notification_progress, download.getProgress()));
        notificationManager.notify(ID, notificationBuilder.build());
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

        item.setDownloading(false);

        sendActionIntent(ACTION_COMPLETE, item);

        notificationManager.cancel(ID);
        notificationBuilder.setProgress(0, 0, false);
        notificationBuilder.setContentText("File Downloaded");
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        notificationBuilder.setOngoing(false);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setContentIntent(PendingIntent
                .getActivity(this, 0, item.getIntent(this), 0));
        notificationManager.notify(ID, notificationBuilder.build());

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        notificationManager.cancel(ID);
    }


    @StringDef({ACTION_STARTED, ACTION_PROGRESS, ACTION_COMPLETE, ACTION_ERROR})
    private @interface Actions {
    }
}
