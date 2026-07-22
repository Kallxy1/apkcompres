package com.kamu.statusmaker;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationRecoveryService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if (!packageName.equals("com.whatsapp") && !packageName.equals("com.whatsapp.w4b")) {
            return;
        }

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = extras.getString(Notification.EXTRA_TITLE);
        CharSequence textChar = extras.getCharSequence(Notification.EXTRA_TEXT);
        String text = textChar != null ? textChar.toString() : "";

        if (title == null || title.isEmpty() || text.isEmpty()) {
            return;
        }

        // Jangan catat notifikasi sistem WhatsApp (seperti "Mengecek pesan baru")
        if (title.equals("WhatsApp") || title.equals("WhatsApp Business") || text.contains("pesan baru")) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences("RecoveryPrefs", Context.MODE_PRIVATE);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // LOGIKA 1: Deteksi Pesan Chat Terhapus (Anti-Tarik Pesan)
        if (text.contains("Pesan ini telah dihapus") || text.contains("This message was deleted")) {
            // Temukan pesan terakhir dari pengirim ini dan tandai sebagai "TERHAPUS"
            String lastMsgKey = "last_msg_" + title;
            String lastMsgText = prefs.getString(lastMsgKey, "");
            if (!lastMsgText.isEmpty() && !lastMsgText.contains("[DELETED]")) {
                String savedHistory = prefs.getString("deleted_chats", "");
                String newDeletedEntry = "[" + time + "] " + title + " (Dihapus): " + lastMsgText + "\n";
                prefs.edit()
                     .putString("deleted_chats", newDeletedEntry + savedHistory)
                     .apply();
            }
        } else {
            // Simpan pesan aktif terakhir untuk perbandingan jika nanti dihapus
            String lastMsgKey = "last_msg_" + title;
            prefs.edit().putString(lastMsgKey, text).apply();

            // Simpan riwayat obrolan masuk secara lokal
            String chatHistory = prefs.getString("chat_history", "");
            String newChatEntry = "[" + time + "] " + title + ": " + text + "\n";
            prefs.edit().putString("chat_history", newChatEntry + chatHistory).apply();
        }

        // LOGIKA 2: Deteksi & Ekstrak Gambar "1x Dilihat" (View Once)
        // Saat pesan 1x dilihat dikirim, WhatsApp memicu notifikasi gambar. Kita bisa menyalin thumbnail-nya!
        if (text.equals("📷 Foto (1x dilihat)") || text.equals("📷 Photo (View once)")) {
            try {
                Bitmap largeIcon = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Icon icon = notification.getLargeIcon();
                    if (icon != null) {
                        // Membutuhkan refleksi atau loading drawable jika di luar package, namun di Android M+
                        // kita bisa mengambil bitmap langsung dari extras jika tersedia.
                    }
                }
                
                if (extras.containsKey(Notification.EXTRA_LARGE_ICON)) {
                    largeIcon = extras.getParcelable(Notification.EXTRA_LARGE_ICON);
                } else if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                    largeIcon = extras.getParcelable(Notification.EXTRA_PICTURE);
                }

                if (largeIcon != null) {
                    File viewOnceDir = new File(getFilesDir(), "recovered_view_once");
                    if (!viewOnceDir.exists()) {
                        viewOnceDir.mkdirs();
                    }
                    
                    File destFile = new File(viewOnceDir, "VIEW_ONCE_" + System.currentTimeMillis() + ".png");
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        largeIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    }
                    
                    // Simpan jejak riwayat foto terpulihkan
                    String viewOnceList = prefs.getString("view_once_list", "");
                    String entry = destFile.getAbsolutePath() + ";" + title + ";" + time + "\n";
                    prefs.edit().putString("view_once_list", entry + viewOnceList).apply();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Notifikasi dihapus dari panel
    }
}
