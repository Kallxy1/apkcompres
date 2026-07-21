package com.kamu.statusmaker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private Button btnSelectVideo;
    private TextView tvSelectedFile;
    private CardView cardProgress;
    private ProgressBar progressBar;
    private TextView tvProgressStatus;
    private Button btnShare;

    private String selectedVideoPath = "";
    private String compressedVideoPath = "";

    // Launcher untuk memilih video dari galeri
    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    if (videoUri != null) {
                        handleSelectedVideo(videoUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi View
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        cardProgress = findViewById(R.id.cardProgress);
        progressBar = findViewById(R.id.progressBar);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);
        btnShare = findViewById(R.id.btnShare);

        // Aksi Tombol Pilih Video
        btnSelectVideo.setOnClickListener(v -> {
            if (checkPermissions()) {
                openVideoPicker();
            } else {
                requestPermissions();
            }
        });

        // Aksi Tombol Share ke WhatsApp
        btnShare.setOnClickListener(v -> {
            if (!compressedVideoPath.isEmpty()) {
                ShareHelper.shareToWhatsAppStatus(MainActivity.this, compressedVideoPath);
            } else {
                Toast.makeText(this, "Video belum siap dibagikan!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Membuka file picker untuk memilih video
    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        videoPickerLauncher.launch(intent);
    }

    // Menangani video yang dipilih oleh pengguna
    private void handleSelectedVideo(Uri uri) {
        try {
            tvSelectedFile.setText("Menyiapkan file video...");
            
            // FFmpeg membutuhkan path file nyata, jadi kita salin URI ke file sementara di cache aplikasi
            File tempFile = new File(getCacheDir(), "temp_input_video.mp4");
            if (tempFile.exists()) {
                tempFile.delete();
            }

            try (InputStream is = getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((is != null) && (bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            selectedVideoPath = tempFile.getAbsolutePath();
            tvSelectedFile.setText("Video terpilih: temp_input_video.mp4\n(Siap untuk dikompresi)");

            // Tentukan lokasi file hasil kompresi di folder cache eksternal
            File outputDir = getExternalFilesDir(null);
            File outputFile = new File(outputDir, "WA_HQ_STATUS_" + System.currentTimeMillis() + ".mp4");
            compressedVideoPath = outputFile.getAbsolutePath();

            // Mulai proses kompresi otomatis
            startCompression();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat video: " + e.getMessage(), Toast.LENGTH_LONG).show();
            tvSelectedFile.setText("Gagal memuat video.");
        }
    }

    // Memulai kompresi video menggunakan kelas VideoCompressor kita
    private void startCompression() {
        VideoCompressor.compressForWhatsApp(selectedVideoPath, compressedVideoPath, new VideoCompressor.CompressionCallback() {
            @Override
            public void onStart() {
                runOnUiThread(() -> {
                    btnSelectVideo.setEnabled(false);
                    btnShare.setVisibility(View.GONE);
                    cardProgress.setVisibility(View.VISIBLE);
                    tvProgressStatus.setText("Sedang mengompresi dengan codec H.264 & Bitrate 3500kbps...");
                });
            }

            @Override
            public void onSuccess(String outputPath) {
                runOnUiThread(() -> {
                    btnSelectVideo.setEnabled(true);
                    cardProgress.setVisibility(View.GONE);
                    btnShare.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Kompresi Berhasil! Siap diupload.", Toast.LENGTH_LONG).show();
                    tvSelectedFile.setText("Status: Sukses Kompres!\nFile tersimpan di folder aplikasi.");
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    btnSelectVideo.setEnabled(true);
                    cardProgress.setVisibility(View.GONE);
                    btnShare.setVisibility(View.GONE);
                    tvSelectedFile.setText("Kompresi Gagal.");
                    Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Cek perizinan (Permissions) berdasarkan versi Android
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Meminta perizinan
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker();
            } else {
                Toast.makeText(this, "Izin penyimpanan ditolak! Aplikasi butuh izin ini untuk mengambil video.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
