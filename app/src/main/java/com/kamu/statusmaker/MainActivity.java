package com.kamu.statusmaker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI Tab Layouts
    private View screenCompress, screenSaver, screenDeleted;
    private LinearLayout tabCompress, tabSaver, tabDeleted;
    private TextView txtTabCompress, txtTabSaver, txtTabDeleted;

    // Tab 1: Compress Views
    private Button btnSelectVideo, btnProcessVideo, btnShare;
    private TextView tvSelectedFile, logStep1, logStep2, logStep3, logStep4;
    private SwitchCompat switchEnhance;
    private CardView cardProgress;
    private ProgressBar progressBar;

    // Tab 2 & 3: Status Views
    private Button btnGrantFolder;
    private GridView gridStatuses, gridDeleted;
    private TextView tvEmptySaver, tvEmptyDeleted;

    // Data
    private String selectedVideoPath = "";
    private String compressedVideoPath = "";
    private ArrayList<File> activeStatusesList = new ArrayList<>();
    private ArrayList<File> deletedStatusesList = new ArrayList<>();

    // SAF Folder Picker Launcher (Untuk akses status WA di Android 11+)
    private final ActivityResultLauncher<Uri> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    // Simpan izin akses folder secara permanen agar tidak minta izin lagi
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    loadWhatsAppStatuses(uri);
                }
            }
    );

    // Video Picker Launcher
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

        // Inisialisasi Layout Utama
        screenCompress = findViewById(R.id.screenCompress);
        screenSaver = findViewById(R.id.screenSaver);
        screenDeleted = findViewById(R.id.screenDeleted);

        tabCompress = findViewById(R.id.tabCompress);
        tabSaver = findViewById(R.id.tabSaver);
        tabDeleted = findViewById(R.id.tabDeleted);

        txtTabCompress = findViewById(R.id.txtTabCompress);
        txtTabSaver = findViewById(R.id.txtTabSaver);
        txtTabDeleted = findViewById(R.id.txtTabDeleted);

        // Inisialisasi Tab 1 (Compress)
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        btnProcessVideo = findViewById(R.id.btnProcessVideo);
        btnShare = findViewById(R.id.btnShare);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        switchEnhance = findViewById(R.id.switchEnhance);
        cardProgress = findViewById(R.id.cardProgress);
        progressBar = findViewById(R.id.progressBar);

        logStep1 = findViewById(R.id.logStep1);
        logStep2 = findViewById(R.id.logStep2);
        logStep3 = findViewById(R.id.logStep3);
        logStep4 = findViewById(R.id.logStep4);

        // Inisialisasi Tab 2 & 3
        btnGrantFolder = findViewById(R.id.btnGrantFolder);
        gridStatuses = findViewById(R.id.gridStatuses);
        gridDeleted = findViewById(R.id.gridDeleted);
        tvEmptySaver = findViewById(R.id.tvEmptySaver);
        tvEmptyDeleted = findViewById(R.id.tvEmptyDeleted);

        // Pasang Navigasi Tab
        tabCompress.setOnClickListener(v -> switchScreen(1));
        tabSaver.setOnClickListener(v -> switchScreen(2));
        tabDeleted.setOnClickListener(v -> switchScreen(3));

        // Listener Aksi Tombol Kompres
        btnSelectVideo.setOnClickListener(v -> {
            if (checkPermissions()) {
                openVideoPicker();
            } else {
                requestPermissions();
            }
        });

        btnProcessVideo.setOnClickListener(v -> {
            if (!selectedVideoPath.isEmpty()) {
                startCompression();
            }
        });

        btnShare.setOnClickListener(v -> {
            if (!compressedVideoPath.isEmpty()) {
                ShareHelper.shareToWhatsAppStatus(MainActivity.this, compressedVideoPath);
            }
        });

        // Listener Status Saver
        btnGrantFolder.setOnClickListener(v -> requestWhatsAppFolderPermission());

        // Otomatis cek folder jika sudah diizinkan sebelumnya
        checkExistingFolderPermission();
    }

    // Toggle Tampilan Antarmuka Tab
    private void switchScreen(int screenNumber) {
        screenCompress.setVisibility(screenNumber == 1 ? View.VISIBLE : View.GONE);
        screenSaver.setVisibility(screenNumber == 2 ? View.VISIBLE : View.GONE);
        screenDeleted.setVisibility(screenNumber == 3 ? View.VISIBLE : View.GONE);

        // Perbaikan: Gunakan setTypeface untuk mengubah ketebalan teks programmatically di Java
        txtTabCompress.setTextColor(ContextCompat.getColor(this, screenNumber == 1 ? R.color.whatsapp_green : R.color.gray_dark));
        txtTabCompress.setTypeface(null, screenNumber == 1 ? Typeface.BOLD : Typeface.NORMAL);

        txtTabSaver.setTextColor(ContextCompat.getColor(this, screenNumber == 2 ? R.color.whatsapp_green : R.color.gray_dark));
        txtTabSaver.setTypeface(null, screenNumber == 2 ? Typeface.BOLD : Typeface.NORMAL);

        txtTabDeleted.setTextColor(ContextCompat.getColor(this, screenNumber == 3 ? R.color.whatsapp_green : R.color.gray_dark));
        txtTabDeleted.setTypeface(null, screenNumber == 3 ? Typeface.BOLD : Typeface.NORMAL);

        if (screenNumber == 2 || screenNumber == 3) {
            checkExistingFolderPermission();
        }
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        videoPickerLauncher.launch(intent);
    }

    private void handleSelectedVideo(Uri uri) {
        try {
            tvSelectedFile.setText("Membaca data video dari penyimpanan...");
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
            tvSelectedFile.setText("Video Siap Diproses:\ntemp_input_video.mp4");
            btnProcessVideo.setVisibility(View.VISIBLE);
            btnShare.setVisibility(View.GONE);
            cardProgress.setVisibility(View.GONE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startCompression() {
        File outputDir = getExternalFilesDir(null);
        File outputFile = new File(outputDir, "WA_STATUS_MAX_" + System.currentTimeMillis() + ".mp4");
        compressedVideoPath = outputFile.getAbsolutePath();

        boolean isEnhanced = switchEnhance.isChecked();

        VideoCompressor.compressForWhatsApp(selectedVideoPath, compressedVideoPath, isEnhanced, new VideoCompressor.CompressionCallback() {
            @Override
            public void onStart() {
                runOnUiThread(() -> {
                    btnSelectVideo.setEnabled(false);
                    btnProcessVideo.setEnabled(false);
                    btnShare.setVisibility(View.GONE);
                    cardProgress.setVisibility(View.VISIBLE);
                    
                    logStep1.setText("● Membaca metadata & orientasi video... (Sukses)");
                    logStep1.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.whatsapp_green));
                    
                    logStep2.setText("○ Menerapkan filter penajam (unsharp)...");
                    logStep2.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.gray_dark));
                    
                    logStep3.setText("○ Mengonversi audio ke standar stereo AAC...");
                    logStep3.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.gray_dark));
                    
                    logStep4.setText("○ Melakukan encoding kompresi presisi x264...");
                    logStep4.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.gray_dark));
                });
            }

            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> {
                    if (message.contains("Penajam")) {
                        logStep2.setText("● Menerapkan filter penajam (unsharp) & kontras... (Sukses)");
                        logStep2.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.whatsapp_green));
                    } else if (message.contains("audio")) {
                        logStep3.setText("● Mengonversi audio ke standar stereo AAC... (Sukses)");
                        logStep3.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.whatsapp_green));
                    } else if (message.contains("encoding")) {
                        logStep4.setText("● Melakukan encoding kompresi presisi x264... (Sedang Berjalan)");
                        logStep4.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.whatsapp_green_light));
                    }
                });
            }

            @Override
            public void onSuccess(String outputPath) {
                runOnUiThread(() -> {
                    btnSelectVideo.setEnabled(true);
                    btnProcessVideo.setEnabled(true);
                    btnProcessVideo.setVisibility(View.GONE);
                    cardProgress.setVisibility(View.GONE);
                    btnShare.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Sukses Menjernihkan & Kompres Video!", Toast.LENGTH_LONG).show();
                    tvSelectedFile.setText("Status: Selesai! Video jauh lebih jernih & siap diunggah.");
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    btnSelectVideo.setEnabled(true);
                    btnProcessVideo.setEnabled(true);
                    cardProgress.setVisibility(View.GONE);
                    tvSelectedFile.setText("Proses gagal.");
                    Toast.makeText(MainActivity.this, "Gagal: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ==========================================
    // BAGIAN FITUR STATUS SAVER & STATUS TERHAPUS
    // ==========================================

    private void requestWhatsAppFolderPermission() {
        // Alamat folder status WA di Android 11 ke atas
        String targetFolder = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses";
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A" + Uri.encode(targetFolder));
        
        try {
            folderPickerLauncher.launch(uri);
        } catch (Exception e) {
            // Fallback jika path tidak didukung oleh file picker bawaan HP
            folderPickerLauncher.launch(null);
        }
    }

    private void checkExistingFolderPermission() {
        List<android.content.UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
        if (permissions != null && !permissions.isEmpty()) {
            for (android.content.UriPermission perm : permissions) {
                if (perm.getUri().toString().contains("com.whatsapp")) {
                    loadWhatsAppStatuses(perm.getUri());
                    return;
                }
            }
        }
        btnGrantFolder.setVisibility(View.VISIBLE);
        gridStatuses.setVisibility(View.GONE);
        tvEmptySaver.setVisibility(View.VISIBLE);
    }

    private void loadWhatsAppStatuses(Uri folderUri) {
        btnGrantFolder.setVisibility(View.GONE);
        tvEmptySaver.setVisibility(View.GONE);

        activeStatusesList.clear();
        DocumentFile rootFolder = DocumentFile.fromTreeUri(this, folderUri);
        
        File backupDir = new File(getFilesDir(), "status_backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        if (rootFolder != null && rootFolder.exists()) {
            DocumentFile[] files = rootFolder.listFiles();
            Set<String> activeFileNames = new HashSet<>();

            for (DocumentFile doc : files) {
                if (doc.isFile() && (doc.getName().endsWith(".jpg") || doc.getName().endsWith(".mp4") || doc.getName().endsWith(".gif"))) {
                    // Salin status aktif ke cache lokal agar bisa dimuat di GridView
                    File cacheFile = copyDocumentToLocal(doc, backupDir);
                    if (cacheFile != null) {
                        activeStatusesList.add(cacheFile);
                        activeFileNames.add(cacheFile.getName());
                    }
                }
            }

            // ISI LOGIKA STATUS TERHAPUS:
            // Jika ada file di folder "status_backups" tetapi namanya sudah tidak terdaftar di status aktif,
            // berarti status tersebut sudah dihapus oleh teman Anda sebelum 24 jam atau sudah kedaluwarsa!
            deletedStatusesList.clear();
            File[] backedFiles = backupDir.listFiles();
            if (backedFiles != null) {
                for (File file : backedFiles) {
                    if (!activeFileNames.contains(file.getName()) && file.isFile()) {
                        deletedStatusesList.add(file);
                    }
                }
            }

            // Tampilkan Grid
            if (!activeStatusesList.isEmpty()) {
                gridStatuses.setVisibility(View.VISIBLE);
                gridStatuses.setAdapter(new StatusAdapter(this, activeStatusesList, false));
                tvEmptySaver.setVisibility(View.GONE);
            } else {
                gridStatuses.setVisibility(View.GONE);
                tvEmptySaver.setVisibility(View.VISIBLE);
                tvEmptySaver.setText("Tidak ada status aktif yang terdeteksi.");
            }

            if (!deletedStatusesList.isEmpty()) {
                gridDeleted.setVisibility(View.VISIBLE);
                gridDeleted.setAdapter(new StatusAdapter(this, deletedStatusesList, true));
                tvEmptyDeleted.setVisibility(View.GONE);
            } else {
                gridDeleted.setVisibility(View.GONE);
                tvEmptyDeleted.setVisibility(View.VISIBLE);
                tvEmptyDeleted.setText("Belum mendeteksi adanya status yang dihapus oleh teman.");
            }
        }
    }

    private File copyDocumentToLocal(DocumentFile doc, File destDir) {
        File destFile = new File(destDir, doc.getName());
        if (destFile.exists()) {
            return destFile; // Sudah di-backup sebelumnya
        }

        try (InputStream is = getContentResolver().openInputStream(doc.getUri());
             OutputStream os = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((is != null) && (bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            return destFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Custom Adapter untuk menampilkan Grid Statuses di HP Kentang (Tanpa lag)
    private static class StatusAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<File> files;
        private final boolean isDeletedMode;

        public StatusAdapter(Context context, ArrayList<File> files, boolean isDeletedMode) {
            this.context = context;
            this.files = files;
            this.isDeletedMode = isDeletedMode;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int position) {
            return files.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.activity_list_item, parent, false);
            }

            ImageView imageView = convertView.findViewById(android.R.id.icon);
            TextView textView = convertView.findViewById(android.R.id.text1);
            
            // Konfigurasi ukuran item grid
            ViewGroup.LayoutParams params = convertView.getLayoutParams();
            if (params == null) {
                params = new GridView.LayoutParams(250, 250);
            } else {
                params.width = 250;
                params.height = 250;
            }
            convertView.setLayoutParams(params);

            File file = files.get(position);
            textView.setText(isDeletedMode ? "TERHAPUS" : "STATUS");
            textView.setTextSize(10); // Perbaikan: Gunakan float murni, bukan literal "10sp"

            // Muat thumbnail gambar/video secara aman demi hemat RAM HP kentang
            if (file.getName().endsWith(".mp4")) {
                Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
                if (thumb != null) {
                    imageView.setImageBitmap(thumb);
                } else {
                    imageView.setImageResource(android.R.drawable.presence_video_online);
                }
            } else {
                Bitmap bitmap = decodeSampledBitmapFromFile(file.getAbsolutePath(), 120, 120);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }

            // Klik item untuk membuka status (simulasi buka foto/video)
            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(context, "com.kamu.statusmaker.fileprovider", file);
                intent.setDataAndType(uri, file.getName().endsWith(".mp4") ? "video/*" : "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            });

            return convertView;
        }

        // Helper untuk dekompresi gambar beresolusi tinggi menjadi pas di GridView (anti Out-Of-Memory)
        private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }

    // Permissions Helper
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

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
                Toast.makeText(this, "Izin penyimpanan ditolak!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
