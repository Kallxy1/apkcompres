package id.xystudio.status;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

public class ShareHelper {

    public static void shareToWhatsAppStatus(Context context, String videoPath) {
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            return;
        }

        // Dapatkan URI yang aman lewat FileProvider
        Uri videoUri = FileProvider.getUriForFile(
            context,
            "id.xystudio.status.fileprovider", // Perbaikan Package Name
            videoFile
        );

        // Intent untuk membagikan video
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/mp4");
        intent.putExtra(Intent.EXTRA_STREAM, videoUri);
        
        // Target langsung ke WhatsApp
        intent.setPackage("com.whatsapp");
        
        // Beri izin bagi penerima (WhatsApp) untuk membaca file kita
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        // Buka WhatsApp
        try {
            context.startActivity(Intent.createChooser(intent, "Bagikan ke Status WhatsApp"));
        } catch (Exception e) {
            e.printStackTrace();
            // Jika WhatsApp biasa tidak terinstall, coba WhatsApp Business
            try {
                intent.setPackage("com.whatsapp.w4b");
                context.startActivity(Intent.createChooser(intent, "Bagikan ke Status WhatsApp"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
