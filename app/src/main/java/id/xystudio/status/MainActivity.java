package id.xystudio.status;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;



import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI Tab Layouts (4 Screens)
    private View screenCompress, screenSaver, screenDeleted, screenSettings;
    private LinearLayout tabCompress, tabSaver, tabDeleted, tabSettings;
    private TextView txtTabCompress, txtTabSaver, txtTabDeleted, txtTabSettings;

    // Dark/Light Theme Views
    private View mainLayout;
    private ViewGroup headerBar, bottomNav;
    private TextView txtThemeMode;
    private SwitchCompat switchTheme;
    private CardView cardSelect, cardEnhance, cardChats, cardClearCache, cardAbout;

    // Tab 1: Compress Views
    private Button btnSelectVideo, btnProcessVideo, btnShare;
    private TextView tvSelectedFile, logStep1, logStep2, logStep3, logStep4;
    private SwitchCompat switchEnhance;
    private CardView cardProgress, cardPreview;
    private ProgressBar progressBar;
    private VideoView videoPreview;

    // Tab 2 & 3: Status & Recovery Views
    private Button btnGrantFolder, btnGrantNotification, btnFilterAll, btnFilterVideo, btnFilterImage;
    private GridView gridStatuses, gridDeleted;
    private TextView tvEmptySaver, tvEmptyDeleted, tvDeletedChatsLog;

    // Tab 4: Settings Views
    private Button btnClearCache;

    // Data
    private String selectedVideoPath = "";
    private String compressedVideoPath = "";
    private ArrayList<File> activeStatusesList = new ArrayList<>();
    private ArrayList<File> filteredStatusesList = new ArrayList<>();
    private ArrayList<File> viewOnceList = new ArrayList<>();
    private int activeFilterMode = 0; // 0 = Semua, 1 = Video Saja, 2 = Gambar Saja

    // Broadcast Receiver untuk mendeteksi selesainya kompresi di Background Service
    private final BroadcastReceiver compressionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra("success", false);
            btnSelectVideo.setEnabled(true);
            btnProcessVideo.setEnabled(true);
            cardProgress.setVisibility(View.GONE);

            if (success) {
                compressedVideoPath = intent.getStringExtra("outputPath");
                btnProcessVideo.setVisibility(View.GONE);
                btnShare.setVisibility(View.VISIBLE);
                tvSelectedFile.setText("Status: Selesai! Video dioptimalkan dalam background.");
                
                // MAIN FEATURE: Tampilkan Preview Video sebelum diupload ke WA!
                cardPreview.setVisibility(View.VISIBLE);
                videoPreview.setVideoPath(compressedVideoPath);
                videoPreview.start();
                videoPreview.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVolume(1f, 1f);
                });
                // Putar video/jeda saat diketuk
                videoPreview.setOnClickListener(v -> {
                    if (videoPreview.isPlaying()) {
                        videoPreview.pause();
                    } else {
                        videoPreview.start();
                    }
                });

                Toast.makeText(MainActivity.this, "Sukses Menjernihkan Video!", Toast.LENGTH_LONG).show();
            } else {
                String error = intent.getStringExtra("error");
                tvSelectedFile.setText("Proses background gagal.");
                Toast.makeText(MainActivity.this, "Gagal: " + error, Toast.LENGTH_LONG).show();
            }
        }
    };

    // SAF Folder Tree Picker Launcher (Android 11+)
    private final ActivityResultLauncher<Uri> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    loadWhatsAppStatusesAndroid11(uri);
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

        // Register receiver kompresi background
        registerReceiver(compressionReceiver, new IntentFilter("id.xystudio.status.COMPRESSION_DONE"));

        // Inisialisasi Layout Utama & Tema
        mainLayout = findViewById(R.id.mainLayout);
        headerBar = findViewById(R.id.headerBar);
        bottomNav = findViewById(R.id.bottomNav);
        txtThemeMode = findViewById(R.id.txtThemeMode);
        switchTheme = findViewById(R.id.switchTheme);

        cardSelect = findViewById(R.id.cardSelect);
        cardEnhance = findViewById(R.id.cardEnhance);
        cardClearCache = findViewById(R.id.cardClearCache);
        cardAbout = findViewById(R.id.cardAbout);

        screenCompress = findViewById(R.id.screenCompress);
        screenSaver = findViewById(R.id.screenSaver);
        screenDeleted = findViewById(R.id.screenDeleted);
        screenSettings = findViewById(R.id.screenSettings);

        tabCompress = findViewById(R.id.tabCompress);
        tabSaver = findViewById(R.id.tabSaver);
        tabDeleted = findViewById(R.id.tabDeleted);
        tabSettings = findViewById(R.id.tabSettings);

        txtTabCompress = findViewById(R.id.txtTabCompress);
        txtTabSaver = findViewById(R.id.txtTabSaver);
        txtTabDeleted = findViewById(R.id.txtTabDeleted);
        txtTabSettings = findViewById(R.id.txtTabSettings);

        // Inisialisasi Tab 1 (Compress)
        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        btnProcessVideo = findViewById(R.id.btnProcessVideo);
        btnShare = findViewById(R.id.btnShare);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        switchEnhance = findViewById(R.id.switchEnhance);
        cardProgress = findViewById(R.id.cardProgress);
        cardPreview = findViewById(R.id.cardPreview);
        progressBar = findViewById(R.id.progressBar);
        videoPreview = findViewById(R.id.videoPreview);

        logStep1 = findViewById(R.id.logStep1);
        logStep2 = findViewById(R.id.logStep2);
        logStep3 = findViewById(R.id.logStep3);
        logStep4 = findViewById(R.id.logStep4);

        // Inisialisasi Tab 2 & 3
        btnGrantFolder = findViewById(R.id.btnGrantFolder);
        btnGrantNotification = findViewById(R.id.btnGrantNotification);
        gridStatuses = findViewById(R.id.gridStatuses);
        gridDeleted = findViewById(R.id.gridDeleted);
        tvEmptySaver = findViewById(R.id.tvEmptySaver);
        tvEmptyDeleted = findViewById(R.id.tvEmptyDeleted);
        tvDeletedChatsLog = findViewById(R.id.tvDeletedChatsLog);
        cardChats = findViewById(R.id.cardChats);

        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterVideo = findViewById(R.id.btnFilterVideo);
        btnFilterImage = findViewById(R.id.btnFilterImage);

        // Inisialisasi Tab 4 (Settings)
        btnClearCache = findViewById(R.id.btnClearCache);

        // Set Up Switch Tema Hitam Putih (Truly Working!)
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> applyMinimalTheme(isChecked));

        // Pasang Navigasi Tab (4 Tabs)
        tabCompress.setOnClickListener(v -> switchScreen(1));
        tabSaver.setOnClickListener(v -> switchScreen(2));
        tabDeleted.setOnClickListener(v -> switchScreen(3));
        tabSettings.setOnClickListener(v -> switchScreen(4));

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
                startBackgroundCompressionService();
            }
        });

        btnShare.setOnClickListener(v -> {
            if (!compressedVideoPath.isEmpty()) {
                ShareHelper.shareToWhatsAppStatus(MainActivity.this, compressedVideoPath);
            }
        });

        // Listener Filter Media di Tab 2
        btnFilterAll.setOnClickListener(v -> changeMediaFilter(0));
        btnFilterVideo.setOnClickListener(v -> changeMediaFilter(1));
        btnFilterImage.setOnClickListener(v -> changeMediaFilter(2));

        // Listener Status Saver
        btnGrantFolder.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestWhatsAppFolderPermissionAndroid11();
            } else {
                loadWhatsAppStatusesLegacy();
            }
        });

        btnGrantNotification.setOnClickListener(v -> {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        });

        // Listener Clear Cache
        btnClearCache.setOnClickListener(v -> performCacheCleanup());

        // Cek status Folder & Notification Access saat dijalankan
        checkStatusSaverAccess();
        loadRecoveredData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(compressionReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Penerapan Tema Hitam Putih Minimalis Programmatic (Tidak restart App, Super Smooth!)
    private void applyMinimalTheme(boolean isDark) {
        int bgColor = isDark ? 0xFF121212 : 0xFFF5F5F5;
        int cardColor = isDark ? 0xFF1E1E1E : 0xFFFFFFFF;
        int textColor = isDark ? 0xFFFFFFFF : 0xFF000000;
        int subTextColor = isDark ? 0xFF888888 : 0xFF333333;

        mainLayout.setBackgroundColor(bgColor);
        headerBar.setBackgroundColor(cardColor);
        bottomNav.setBackgroundColor(cardColor);

        txtThemeMode.setText(isDark ? "DARK" : "LIGHT");
        txtThemeMode.setTextColor(textColor);

        // Update semua tulisan judul & desc
        TextView hTitle = (TextView) headerBar.getChildAt(0);
        hTitle.setTextColor(textColor);

        // Update Card background
        cardSelect.setCardBackgroundColor(cardColor);
        cardEnhance.setCardBackgroundColor(cardColor);
        cardProgress.setCardBackgroundColor(cardColor);
        cardPreview.setCardBackgroundColor(cardColor);
        cardChats.setCardBackgroundColor(cardColor);
        cardClearCache.setCardBackgroundColor(cardColor);
        cardAbout.setCardBackgroundColor(cardColor);

        // Update tulisan di Compress Card
        ((TextView) findViewById(R.id.lblTitle1)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblDesc1)).setTextColor(subTextColor);
        ((TextView) findViewById(R.id.lblTitle2)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblDesc2)).setTextColor(subTextColor);
        ((TextView) findViewById(R.id.lblProgress)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblPreviewTitle)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblPreviewDesc)).setTextColor(subTextColor);

        tvSelectedFile.setTextColor(subTextColor);
        logStep1.setTextColor(subTextColor);
        logStep2.setTextColor(subTextColor);
        logStep3.setTextColor(subTextColor);
        logStep4.setTextColor(subTextColor);

        // Update Saver & Recovery Card
        ((TextView) findViewById(R.id.lblTitleSaver)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblDescSaver)).setTextColor(subTextColor);
        ((TextView) findViewById(R.id.lblTitleRecovery)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblDescRecovery)).setTextColor(subTextColor);
        ((TextView) findViewById(R.id.lblSubViewOnce)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblSubChats)).setTextColor(textColor);

        // Settings Card
        ((TextView) findViewById(R.id.lblTitleSettings)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblTitleCache)).setTextColor(textColor);
        ((TextView) findViewById(R.id.lblDescCache)).setTextColor(subTextColor);
        ((TextView) findViewById(R.id.lblTitleAbout)).setTextColor(textColor);
        ((TextView) findViewById(R.id.tvAboutDev)).setTextColor(subTextColor);
        ((TextView) findViewById(R.id.lblChangelog)).setTextColor(subTextColor);

        tvEmptySaver.setTextColor(subTextColor);
        tvEmptyDeleted.setTextColor(subTextColor);
        tvDeletedChatsLog.setTextColor(subTextColor);

        // Ganti warna tab teks
        txtTabCompress.setTextColor(isDark ? 0xFFFFFFFF : 0xFF000000);
        txtTabSaver.setTextColor(isDark ? 0xFFFFFFFF : 0xFF000000);
        txtTabDeleted.setTextColor(isDark ? 0xFFFFFFFF : 0xFF000000);
        txtTabSettings.setTextColor(isDark ? 0xFFFFFFFF : 0xFF000000);
    }

    private void switchScreen(int screenNumber) {
        screenCompress.setVisibility(screenNumber == 1 ? View.VISIBLE : View.GONE);
        screenSaver.setVisibility(screenNumber == 2 ? View.VISIBLE : View.GONE);
        screenDeleted.setVisibility(screenNumber == 3 ? View.VISIBLE : View.GONE);
        screenSettings.setVisibility(screenNumber == 4 ? View.VISIBLE : View.GONE);

        txtTabCompress.setTypeface(null, screenNumber == 1 ? Typeface.BOLD : Typeface.NORMAL);
        txtTabSaver.setTypeface(null, screenNumber == 2 ? Typeface.BOLD : Typeface.NORMAL);
        txtTabDeleted.setTypeface(null, screenNumber == 3 ? Typeface.BOLD : Typeface.NORMAL);
        txtTabSettings.setTypeface(null, screenNumber == 4 ? Typeface.BOLD : Typeface.NORMAL);

        if (screenNumber == 2) {
            checkStatusSaverAccess();
        } else if (screenNumber == 3) {
            loadRecoveredData();
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
            cardPreview.setVisibility(View.GONE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Memulai Kompresi di Background Service (Aman saat App ditutup)
    private void startBackgroundCompressionService() {
        File outputDir = getExternalFilesDir(null);
        File outputFile = new File(outputDir, "WA_STATUS_MAX_" + System.currentTimeMillis() + ".mp4");
        compressedVideoPath = outputFile.getAbsolutePath();

        Intent serviceIntent = new Intent(this, CompressionService.class);
        serviceIntent.putExtra("inputPath", selectedVideoPath);
        serviceIntent.putExtra("outputPath", compressedVideoPath);
        serviceIntent.putExtra("isEnhanced", switchEnhance.isChecked());

        btnSelectVideo.setEnabled(false);
        btnProcessVideo.setEnabled(false);
        cardProgress.setVisibility(View.VISIBLE);
        cardPreview.setVisibility(View.GONE);
        logStep1.setText("● Memulai optimasi video di latar belakang... (Sukses)");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // ==========================================
    // SINKRONISASI STATUS WA DUAL-PATH (LEGACY & ANDROID 11+)
    // ==========================================

    private void checkStatusSaverAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            List<android.content.UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
            if (permissions != null && !permissions.isEmpty()) {
                for (android.content.UriPermission perm : permissions) {
                    if (perm.getUri().toString().contains("com.whatsapp")) {
                        loadWhatsAppStatusesAndroid11(perm.getUri());
                        return;
                    }
                }
            }
            btnGrantFolder.setVisibility(View.VISIBLE);
            gridStatuses.setVisibility(View.GONE);
            tvEmptySaver.setVisibility(View.VISIBLE);
        } else {
            btnGrantFolder.setVisibility(View.GONE);
            loadWhatsAppStatusesLegacy();
        }
    }

    private void requestWhatsAppFolderPermissionAndroid11() {
        String targetFolder = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses";
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A" + Uri.encode(targetFolder));
        try {
            folderPickerLauncher.launch(uri);
        } catch (Exception e) {
            folderPickerLauncher.launch(null);
        }
    }

    private void loadWhatsAppStatusesLegacy() {
        activeStatusesList.clear();
        String[] possiblePaths = {
            Environment.getExternalStorageDirectory() + "/WhatsApp/Media/.Statuses",
            Environment.getExternalStorageDirectory() + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            Environment.getExternalStorageDirectory() + "/WhatsApp Business/Media/.Statuses",
            Environment.getExternalStorageDirectory() + "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
        };

        for (String path : possiblePaths) {
            File folder = new File(path);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".mp4") || file.getName().endsWith(".gif"))) {
                            activeStatusesList.add(file);
                        }
                    }
                }
            }
        }
        applyFilterAndDisplay();
    }

    private void loadWhatsAppStatusesAndroid11(Uri folderUri) {
        activeStatusesList.clear();
        DocumentFile rootFolder = DocumentFile.fromTreeUri(this, folderUri);
        File backupDir = new File(getFilesDir(), "status_backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        if (rootFolder != null && rootFolder.exists()) {
            DocumentFile[] files = rootFolder.listFiles();
            for (DocumentFile doc : files) {
                if (doc.isFile() && (doc.getName().endsWith(".jpg") || doc.getName().endsWith(".mp4") || doc.getName().endsWith(".gif"))) {
                    File cacheFile = copyDocumentToLocal(doc, backupDir);
                    if (cacheFile != null) {
                        activeStatusesList.add(cacheFile);
                    }
                }
            }
            applyFilterAndDisplay();
        }
    }

    // Memfilter data media di Tab 2 (All, Video, Image)
    private void changeMediaFilter(int mode) {
        activeFilterMode = mode;
        btnFilterAll.setBackgroundTintList(ContextCompat.getColorStateList(this, mode == 0 ? R.color.black : R.color.gray_light));
        btnFilterAll.setTextColor(ContextCompat.getColor(this, mode == 0 ? R.color.white : R.color.gray_dark));

        btnFilterVideo.setBackgroundTintList(ContextCompat.getColorStateList(this, mode == 1 ? R.color.black : R.color.gray_light));
        btnFilterVideo.setTextColor(ContextCompat.getColor(this, mode == 1 ? R.color.white : R.color.gray_dark));

        btnFilterImage.setBackgroundTintList(ContextCompat.getColorStateList(this, mode == 2 ? R.color.black : R.color.gray_light));
        btnFilterImage.setTextColor(ContextCompat.getColor(this, mode == 2 ? R.color.white : R.color.gray_dark));

        applyFilterAndDisplay();
    }

    private void applyFilterAndDisplay() {
        filteredStatusesList.clear();
        for (File f : activeStatusesList) {
            if (activeFilterMode == 0) {
                filteredStatusesList.add(f);
            } else if (activeFilterMode == 1 && f.getName().endsWith(".mp4")) {
                filteredStatusesList.add(f);
            } else if (activeFilterMode == 2 && (f.getName().endsWith(".jpg") || f.getName().endsWith(".gif"))) {
                filteredStatusesList.add(f);
            }
        }

        if (!filteredStatusesList.isEmpty()) {
            gridStatuses.setVisibility(View.VISIBLE);
            gridStatuses.setAdapter(new StatusAdapter(this, filteredStatusesList, false));
            tvEmptySaver.setVisibility(View.GONE);
        } else {
            gridStatuses.setVisibility(View.GONE);
            tvEmptySaver.setVisibility(View.VISIBLE);
            tvEmptySaver.setText("Tidak ada media yang cocok dengan filter.");
        }
    }

    private File copyDocumentToLocal(DocumentFile doc, File destDir) {
        File destFile = new File(destDir, doc.getName());
        if (destFile.exists()) {
            return destFile;
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

    // ==========================================
    // PEMULIHAN CHAT TERHAPUS & FOTO 1X LIHAT
    // ==========================================

    private void loadRecoveredData() {
        SharedPreferences prefs = getSharedPreferences("RecoveryPrefs", Context.MODE_PRIVATE);

        // 1. Muat riwayat Chat Terhapus
        String deletedChats = prefs.getString("deleted_chats", "");
        if (!deletedChats.isEmpty()) {
            tvDeletedChatsLog.setText(deletedChats);
        } else {
            tvDeletedChatsLog.setText("Belum mendeteksi adanya pesan obrolan yang dihapus oleh teman.");
        }

        // 2. Muat Foto 1x Lihat Terpulihkan
        viewOnceList.clear();
        File viewOnceDir = new File(getFilesDir(), "recovered_view_once");
        if (viewOnceDir.exists() && viewOnceDir.isDirectory()) {
            File[] files = viewOnceDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".png")) {
                        viewOnceList.add(f);
                    }
                }
            }
        }

        if (!viewOnceList.isEmpty()) {
            gridDeleted.setVisibility(View.VISIBLE);
            gridDeleted.setAdapter(new StatusAdapter(this, viewOnceList, true));
            tvEmptyDeleted.setVisibility(View.GONE);
        } else {
            gridDeleted.setVisibility(View.GONE);
            tvEmptyDeleted.setVisibility(View.VISIBLE);
        }

        // 3. Update Status Tombol Notification Access
        boolean isNotificationAccessGranted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());
        btnGrantNotification.setVisibility(isNotificationAccessGranted ? View.GONE : View.VISIBLE);
    }

    // ==========================================
    // PENGATURAN: PEMBERSIH CACHE MANDIRI
    // ==========================================

    private void performCacheCleanup() {
        try {
            File outputDir = getExternalFilesDir(null);
            int deletedFilesCount = 0;
            if (outputDir != null && outputDir.isDirectory()) {
                File[] files = outputDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().startsWith("WA_STATUS_MAX")) {
                            if (f.delete()) {
                                deletedFilesCount++;
                            }
                        }
                    }
                }
            }
            Toast.makeText(this, "Sukses membersihkan " + deletedFilesCount + " cache video lama!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membersihkan cache.", Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // CUSTOM ADAPTER STATUS
    // ==========================================

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

            ViewGroup.LayoutParams params = convertView.getLayoutParams();
            if (params == null) {
                params = new GridView.LayoutParams(250, 250);
            } else {
                params.width = 250;
                params.height = 250;
            }
            convertView.setLayoutParams(params);

            File file = files.get(position);
            textView.setText(isDeletedMode ? "TERPULIHKAN" : "STATUS");
            textView.setTextSize(10);

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

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(context, "id.xystudio.status.fileprovider", file); // Perbaikan Package Name
                intent.setDataAndType(uri, file.getName().endsWith(".mp4") ? "video/*" : "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            });

            return convertView;
        }

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
