package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public final class HelpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getWindow().setStatusBarColor(getColor(R.color.home_background));
        getWindow().setNavigationBarColor(getColor(R.color.home_background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        boolean contact = getIntent().getBooleanExtra("show_contact", false);
        findViewById(R.id.helpAboutSection).setVisibility(contact ? View.GONE : View.VISIBLE);
        findViewById(R.id.helpContactSection).setVisibility(contact ? View.VISIBLE : View.GONE);
        findViewById(R.id.helpBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.helpFeedbackButton).setOnClickListener(view -> {
            if (!new com.lostlink.app.data.AuthStore(this).isLoggedIn()) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                startActivity(new Intent(this, FeedbackActivity.class));
            }
        });
        findViewById(R.id.helpEmailButton).setOnClickListener(view ->
                openExternal(new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("mailto:lostfoundmanagement22@gmail.com"))));
        findViewById(R.id.helpCallButton).setOnClickListener(view ->
                openExternal(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))));
    }

    private void openExternal(Intent intent) {
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, R.string.help_action_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }
}
