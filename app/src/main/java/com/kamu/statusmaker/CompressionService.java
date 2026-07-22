package com.kamu.statusmaker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.io.File;

public class CompressionService extends Service {

    private static final String CHANNEL_ID = "CompressionServiceChannel";
    private static final int NOTIFICATION_ID = 5001;

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String inputPath = intent.getStringExtra("inputPath");
        String outputPath = intent.getStringExtra("outputPath");
        boolean isEnhanced = intent.getBooleanExtra("isEnhanced", true);

        // Buat Notification untuk Foreground Service agar sistem tidak mematikan app saat di-background
        Notification notification = createProgressNotification("Menyiapkan video untuk dioptimasi...", 0);
        startForeground(NOTIFICATION_ID, notification);

        // Jalankan kompresi di Background Thread agar aman
        new Thread(() -> {
            VideoCompressor.compressForWhatsApp(inputPath, outputPath, isEnhanced, new VideoCompressor.CompressionCallback() {
                @Override
                public void onStart() {
                    updateNotification("Memulai optimasi video...", 10);
                }

                @Override
                public void onProgress(String message) {
                    updateNotification(message, 40);
                }

                @Override
                public void onSuccess(String outputPath) {
                    updateNotificationSuccess("Sukses! Video siap diunggah ke status WA.", outputPath);
                    
                    // Beri tahu Activity utama lewat broadcast lokal
                    Intent broadcast = new Intent("com.kamu.statusmaker.COMPRESSION_DONE");
                    broadcast.putExtra("success", true);
                    broadcast.putExtra("outputPath", outputPath);
                    sendBroadcast(broadcast);
                    
                    stopSelf();
                }

                @Override
                public void onFailure(String errorMessage) {
                    updateNotificationFailure("Gagal mengompresi: " + errorMessage);
                    
                    Intent broadcast = new Intent("com.kamu.statusmaker.COMPRESSION_DONE");
                    broadcast.putExtra("success", false);
                    broadcast.putExtra("error", errorMessage);
                    sendBroadcast(broadcast);
                    
                    stopSelf();
                }
            });
        }).start();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Optimasi Video Background",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createProgressNotification(String contentText, int progress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("StatusMax Pro - Sedang Bekerja")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, progress == 0)
                .setContentIntent(pendingIntent)
                .setColor(0xFF25D366)
                .build();
    }

    private void updateNotification(String text, int progress) {
        Notification notification = createProgressNotification(text, progress);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void updateNotificationSuccess(String text, String outputPath) {
        Intent shareIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, shareIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("StatusMax Pro - Selesai!")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(0xFF25D366)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void updateNotificationFailure(String errorText) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("StatusMax Pro - Gagal")
                .setContentText(errorText)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
