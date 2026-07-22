package com.kamu.statusmaker;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

public class VideoCompressor {

    public interface CompressionCallback {
        void onStart();
        void onProgress(String message);
        void onSuccess(String outputPath);
        void onFailure(String errorMessage);
    }

    public static void compressForWhatsApp(String inputPath, String outputPath, boolean isEnhanced, CompressionCallback callback) {
        callback.onStart();

        // 1. Tentukan video filter berdasarkan mode yang dipilih
        String videoFilter;
        if (isEnhanced) {
            callback.onProgress("Mengaktifkan filter Penajam (unsharp) & Penyesuai Kontras...");
            // - unsharp=5:5:1.5:5:5:0.0 : Mempertajam gambar (sharpening) secara agresif agar detail halus tidak burik
            // - eq=contrast=1.15:brightness=0.02:saturation=1.1 : Menaikkan kontras & saturasi agar warna lebih pop & tajam di layar HP
            videoFilter = "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2," +
                          "unsharp=5:5:1.5:5:5:0.0,eq=contrast=1.15:brightness=0.02:saturation=1.1,format=yuv420p";
        } else {
            callback.onProgress("Menggunakan filter kompresi standar...");
            videoFilter = "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2,format=yuv420p";
        }

        // 2. Susun perintah FFmpeg dengan spesifikasi optimal
        String ffmpegCommand = String.format(
            "-y -i \"%s\" -t 30 -vf \"%s\" " +
            "-c:v libx264 -profile:v high -level 4.1 -b:v 3500k -maxrate 3800k -bufsize 3500k " +
            "-c:a aac -b:a 128k -movflags +faststart \"%s\"",
            inputPath, videoFilter, outputPath
        );

        callback.onProgress("Memulai encoding video menggunakan arsitektur CPU HP Anda...");

        // 3. Jalankan pemrosesan FFmpeg Kit secara asinkron
        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            ReturnCode returnCode = session.getReturnCode();

            if (ReturnCode.isSuccess(returnCode)) {
                callback.onSuccess(outputPath);
            } else if (ReturnCode.isCancel(returnCode)) {
                callback.onFailure("Pemrosesan video dibatalkan.");
            } else {
                String errorLog = session.getFailStackTrace();
                if (errorLog == null || errorLog.isEmpty()) {
                    errorLog = "Gagal memproses video. Format video tidak didukung atau resolusi terlalu ekstrem.";
                }
                callback.onFailure(errorLog);
            }
        });
    }
}
