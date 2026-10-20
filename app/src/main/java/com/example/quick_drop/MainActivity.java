package com.example.quick_drop;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // We removed the padding to allow fragments to handle edge-to-edge content.
            // Fragments that need padding should handle insets themselves.
            return insets;
        });

        handleSplash();
    }

    private void handleSplash() {
        View splashScreen = findViewById(R.id.splash_screen);
        View splashLogo = findViewById(R.id.splash_logo);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.splash_anim);
        splashLogo.startAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                splashScreen.postDelayed(() -> {
                    splashScreen.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() -> splashScreen.setVisibility(View.GONE))
                        .start();
                }, 1000);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }
}