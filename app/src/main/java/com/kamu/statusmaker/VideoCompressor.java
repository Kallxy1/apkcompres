package com.kamu.statusmaker;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

public class VideoCompressor {

    public interface CompressionCallback {
        void onStart();
        void onSuccess(String outputPath);
        void onFailure(String errorMessage);
    }

    public static void compressForWhatsApp(String inputPath, String outputPath, CompressionCallback callback) {
        callback.onStart();

        // Formula emas untuk mengoptimalkan video status WhatsApp:
        // - Berorientasi portrait 1080x1920 (menggunakan scale dan padding yang adaptif)
        // - Menggunakan codec H.264 (libx264) & Profile High 4.1
        // - Batas bitrate video stabil di ~3500k agar tidak dicompress lagi oleh WA
        // - Durasi dipotong maksimal 30 detik
        // - Audio dikodekan ke AAC 128k
        String ffmpegCommand = String.format(
            "-y -i \"%s\" -t 30 -vf \"scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2,format=yuv420p\" " +
            "-c:v libx264 -profile:v high -level 4.1 -b:v 3500k -maxrate 3800k -bufsize 3500k " +
            "-c:a aac -b:a 128k -movflags +faststart \"%s\"",
            inputPath, outputPath
        );

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            ReturnCode returnCode = session.getReturnCode();

            if (ReturnCode.isSuccess(returnCode)) {
                callback.onSuccess(outputPath);
            } else if (ReturnCode.isCancel(returnCode)) {
                callback.onFailure("Proses kompresi dibatalkan.");
            } else {
                String errorLog = session.getFailStackTrace();
                if (errorLog == null || errorLog.isEmpty()) {
                    errorLog = "Gagal memproses video. Pastikan format video Anda valid.";
                }
                callback.onFailure(errorLog);
            }
        });
    }
}
