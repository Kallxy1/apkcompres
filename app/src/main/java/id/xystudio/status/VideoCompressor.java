package id.xystudio.status;

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
            // - unsharp=3:3:0.8:3:3:0.0 : Mempertajam gambar (sharpening) ringan agar detail halus tidak burik & lancar di HP 32-bit
            // - eq=contrast=1.05:saturation=1.05 : Menaikkan kontras & saturasi secara proporsional agar warna lebih pop & tajam di layar HP
            videoFilter = "scale=iw:ih,unsharp=3:3:0.8:3:3:0.0,eq=contrast=1.05:saturation=1.05,format=yuv420p";
        } else {
            callback.onProgress("Menggunakan filter kompresi standar...");
            videoFilter = "scale=iw:ih,format=yuv420p";
        }

        // 2. Susun perintah FFmpeg dengan spesifikasi optimal (menjaga resolusi & FPS asli agar tidak lag!)
        String ffmpegCommand = String.format(
            "-y -i \"%s\" -t 30 -vf \"%s\" " +
            "-c:v libx264 -preset superfast -b:v 3000k -maxrate 3500k -bufsize 3000k " +
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
