package com.lostlink.app;

import android.app.Activity;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostLinkDatabase;

public class ProfileActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 513;
    private AuthStore authStore;
    private LostLinkDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        authStore = new AuthStore(this);
        database = new LostLinkDatabase(this);
        database.saveUser(authStore.getEmail(), authStore.getDisplayName());
        ((TextView) findViewById(R.id.profileName)).setText(authStore.getDisplayName());
        ((TextView) findViewById(R.id.profileEmail)).setText(authStore.getEmail());
        ((TextView) findViewById(R.id.profileReportCount)).setText(
                String.valueOf(database.countItems(authStore.getEmail(), null)));
        ((EditText) findViewById(R.id.profileEditName)).setText(authStore.getDisplayName());
        ((EditText) findViewById(R.id.profileEditUsername)).setText(authStore.getUsername());
        ((EditText) findViewById(R.id.profileEditMobile)).setText(authStore.getMobile());
        ((EditText) findViewById(R.id.profileEditAddress)).setText(authStore.getAddress());
        findViewById(R.id.profileBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.profileSaveButton).setOnClickListener(view -> saveProfile());
        findViewById(R.id.profileChangePasswordButton).setOnClickListener(view ->
                showChangePasswordDialog());
        findViewById(R.id.profileReportsButton).setOnClickListener(view -> {
            Intent intent = new Intent(this, ItemsActivity.class);
            intent.putExtra("my_reports", true);
            startActivity(intent);
        });
        findViewById(R.id.profileClaimsButton).setOnClickListener(view ->
                startActivity(new Intent(this, ClaimsActivity.class)));
        findViewById(R.id.profileNotificationsButton).setOnClickListener(view ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.profileFeedbackButton).setOnClickListener(view ->
                startActivity(new Intent(this, FeedbackActivity.class)));
        findViewById(R.id.profileAboutButton).setOnClickListener(view ->
                startActivity(new Intent(this, HelpActivity.class)));
        findViewById(R.id.profileContactButton).setOnClickListener(view -> {
            Intent intent = new Intent(this, HelpActivity.class);
            intent.putExtra("show_contact", true);
            startActivity(intent);
        });
        findViewById(R.id.profileMessagesButton).setOnClickListener(view ->
                startActivity(new Intent(this, SystemMessagesActivity.class)));
        findViewById(R.id.enableNotificationsButton).setOnClickListener(view ->
                enableNotifications());
        View adminReviewButton = findViewById(R.id.profileAdminReviewButton);
        adminReviewButton.setVisibility(authStore.isAdmin() ? View.VISIBLE : View.GONE);
        View adminConsoleButton = findViewById(R.id.profileAdminConsoleButton);
        adminConsoleButton.setVisibility(authStore.isAdmin() ? View.VISIBLE : View.GONE);
        adminConsoleButton.setOnClickListener(view ->
                startActivity(new Intent(this, AdminConsoleActivity.class)));
        adminReviewButton.setOnClickListener(view ->
                startActivity(new Intent(this, AdminReviewActivity.class)));
        Button logoutButton = findViewById(R.id.profileLogoutButton);
        logoutButton.setOnClickListener(view -> {
            authStore.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveProfile() {
        String name = ((EditText) findViewById(R.id.profileEditName)).getText().toString().trim();
        String username = ((EditText) findViewById(R.id.profileEditUsername)).getText().toString().trim();
        String mobile = ((EditText) findViewById(R.id.profileEditMobile)).getText().toString().trim();
        String address = ((EditText) findViewById(R.id.profileEditAddress)).getText().toString().trim();
        if (!authStore.updateProfile(name, username, mobile, address)) {
            android.widget.Toast.makeText(this, R.string.profile_save_failed,
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        database.saveUserProfile(authStore.getEmail(), name, mobile, address, username);
        ((TextView) findViewById(R.id.profileName)).setText(name);
        android.widget.Toast.makeText(this, R.string.profile_saved,
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void showChangePasswordDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, padding, padding, 0);
        EditText current = passwordField(R.string.current_password);
        EditText newPassword = passwordField(R.string.new_password);
        EditText confirm = passwordField(R.string.confirm_new_password);
        form.addView(current);
        form.addView(newPassword);
        form.addView(confirm);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.change_password, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (!newPassword.getText().toString().equals(confirm.getText().toString())) {
                        confirm.setError(getString(R.string.passwords_do_not_match));
                        return;
                    }
                    if (!authStore.changePassword(current.getText().toString(),
                            newPassword.getText().toString())) {
                        android.widget.Toast.makeText(this, R.string.password_change_failed,
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    android.widget.Toast.makeText(this, R.string.password_changed,
                            android.widget.Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private EditText passwordField(int hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return input;
    }

    private void enableNotifications() {
        if (Build.VERSION.SDK_INT < 33) {
            android.widget.Toast.makeText(this, R.string.notification_permission_enabled,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            android.widget.Toast.makeText(this, R.string.notification_permission_enabled,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_NOTIFICATIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS) return;
        int message = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ? R.string.notification_permission_enabled : R.string.notification_permission_denied;
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
