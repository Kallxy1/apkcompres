package id.xystudio.status;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView imgBall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        imgBall = findViewById(R.id.imgBall);

        // Tunggu layout terpasang agar dimensi layar terbaca akurat
        imgBall.post(() -> {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float screenHeight = metrics.heightPixels;

            // 1. Tempatkan bola di luar atas layar terlebih dahulu
            imgBall.setTranslationY(-screenHeight / 2 - 100);

            // 2. Animasi Bola jatuh ke tengah layar (dengan efek memantul/Bounce)
            imgBall.animate()
                    .translationY(0)
                    .setDuration(1200)
                    .setInterpolator(new BounceInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // Tunggu sejenak setelah bola mendarat sebelum membesar
                            imgBall.postDelayed(() -> expandAndNext(), 400);
                        }
                    })
                    .start();
        });
    }

    // 3. Animasi Bola Membesar menutupi seluruh layar secara dramatis
    private void expandAndNext() {
        imgBall.animate()
                .scaleX(35f)
                .scaleY(35f)
                .setDuration(800)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        // Mulai MainActivity dengan lancar
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                })
                .start();
    }
}
