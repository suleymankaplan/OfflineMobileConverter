package com.example.offlinemobileconverter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.arthenica.ffmpegkit.FFmpegKit;

public class ConversionService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";
    public static final String EXTRA_PROGRESS = "EXTRA_PROGRESS";
    public static final String EXTRA_TITLE = "EXTRA_TITLE";

    private static final String CHANNEL_ID = "ConversionChannel";
    private static final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                String title = intent.getStringExtra(EXTRA_TITLE);
                startForegroundServiceWithNotification(title != null ? title : "Dönüştürülüyor...");
            } else if (ACTION_UPDATE.equals(action)) {
                int progress = intent.getIntExtra(EXTRA_PROGRESS, 0);
                updateNotification(progress);
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(true);
                stopSelf();
            } else if (ACTION_CANCEL.equals(action)) {
                FFmpegKit.cancel();
                Intent broadcastIntent = new Intent("ACTION_CANCEL_CONVERSION");
                sendBroadcast(broadcastIntent);
                stopForeground(true);
                stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    private void startForegroundServiceWithNotification(String title) {
        Intent cancelIntent = new Intent(this, ConversionService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingCancelIntent = PendingIntent.getService(
                this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Offline Converter")
                .setContentText(title)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, 0, false)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, "İptal Et", pendingCancelIntent);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateNotification(int progress) {
        if (notificationBuilder != null && notificationManager != null) {
            notificationBuilder.setProgress(100, progress, false);
            notificationBuilder.setContentText("%" + progress + " Tamamlandı");
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Dönüştürme İşlemleri", NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        FFmpegKit.cancel();
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}