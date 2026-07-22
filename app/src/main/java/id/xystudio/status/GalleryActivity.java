package id.xystudio.status;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.kamu.statusmaker.R;

import java.io.File;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity {

    private GridView gridGallery;
    private ProgressBar progressLoading;
    private TextView tvEmptyGallery;
    private TextView btnBack;

    private ArrayList<String> videoPaths = new ArrayList<>();
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        gridGallery = findViewById(R.id.gridGallery);
        progressLoading = findViewById(R.id.progressLoading);
        tvEmptyGallery = findViewById(R.id.tvEmptyGallery);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Muat video dari penyimpanan di background thread
        new Thread(this::loadVideosFromStorage).start();
    }

    private void loadVideosFromStorage() {
        videoPaths.clear();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.VideoColumns.DATA};
        String orderBy = MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC";

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, orderBy)) {
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATA);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(columnIndex);
                    if (path != null && new File(path).exists()) {
                        videoPaths.add(path);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        runOnUiThread(() -> {
            progressLoading.setVisibility(View.GONE);
            if (!videoPaths.isEmpty()) {
                gridGallery.setVisibility(View.VISIBLE);
                adapter = new GalleryAdapter(this, videoPaths);
                gridGallery.setAdapter(adapter);
            } else {
                tvEmptyGallery.setVisibility(View.VISIBLE);
            }
        });
    }

    private static class GalleryAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<String> paths;

        public GalleryAdapter(Context context, ArrayList<String> paths) {
            this.context = context;
            this.paths = paths;
        }

        @Override
        public int getCount() {
            return paths.size();
        }

        @Override
        public Object getItem(int position) {
            return paths.get(position);
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

            // Atur ukuran grid item
            ViewGroup.LayoutParams params = convertView.getLayoutParams();
            if (params == null) {
                params = new GridView.LayoutParams(250, 250);
            } else {
                params.width = 250;
                params.height = 250;
            }
            convertView.setLayoutParams(params);

            String path = paths.get(position);
            File file = new File(path);
            textView.setText(file.getName());
            textView.setTextSize(9);

            // Buat thumbnail di background atau muat mini kind secara efisien
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
            if (thumb != null) {
                imageView.setImageBitmap(thumb);
            } else {
                imageView.setImageResource(android.R.drawable.presence_video_online);
            }

            convertView.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedVideoPath", path);
                ((GalleryActivity) context).setResult(RESULT_OK, resultIntent);
                ((GalleryActivity) context).finish();
            });

            return convertView;
        }
    }
}
