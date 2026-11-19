package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;

public class SplashActivity extends Activity {
    private static final long SPLASH_DURATION_MS = 1450L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable continueToApp = this::openNextScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.navy));
        getWindow().setNavigationBarColor(getColor(R.color.navy));
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.splashLogo);
        ImageView mark = findViewById(R.id.splashMark);
        TextView title = findViewById(R.id.splashTitle);
        TextView subtitle = findViewById(R.id.splashSubtitle);

        logo.setScaleX(0.84f);
        logo.setScaleY(0.84f);
        logo.setAlpha(0f);
        title.setAlpha(0f);
        subtitle.setAlpha(0f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(650L).setInterpolator(new DecelerateInterpolator()).start();
        title.animate().alpha(1f).setStartDelay(250L).setDuration(500L).start();
        subtitle.animate().alpha(1f).setStartDelay(400L).setDuration(500L).start();
        mark.setContentDescription(getString(R.string.app_name));

        handler.postDelayed(continueToApp, SPLASH_DURATION_MS);
    }

    private void openNextScreen() {
        Intent intent = new Intent(this,
                new AuthStore(this).isLoggedIn() ? DashboardActivity.class : HomeActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(continueToApp);
        super.onDestroy();
    }
}
