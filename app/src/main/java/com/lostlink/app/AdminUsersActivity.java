package com.lostlink.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostLinkDatabase;

import java.util.ArrayList;

public final class AdminUsersActivity extends Activity {
    private AuthStore authStore;
    private LostLinkDatabase database;
    private LinearLayout usersList;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);
        configureSystemBars();
        authStore = new AuthStore(this);
        if (!authStore.isAdmin()) {
            Toast.makeText(this, R.string.admin_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        database = new LostLinkDatabase(this);
        ((TextView) findViewById(R.id.adminListTitle)).setText(R.string.admin_users_title);
        ((TextView) findViewById(R.id.adminListEmpty)).setText(R.string.admin_users_empty);
        usersList = findViewById(R.id.adminListContent);
        emptyMessage = findViewById(R.id.adminListEmpty);
        findViewById(R.id.adminListBackButton).setOnClickListener(view -> finish());
        refreshUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshUsers();
    }

    private void refreshUsers() {
        usersList.removeAllViews();
        ArrayList<String[]> users = authStore.getStudentAccounts();
        for (String[] user : users) {
            LinearLayout card = card();
            addText(card, user[1], 17, true);
            addText(card, user[0], 13, false);
            if (!user[2].isEmpty()) addText(card, getString(R.string.mobile_value, user[2]), 13, false);
            if (!user[3].isEmpty()) addText(card, getString(R.string.address_value, user[3]), 13, false);
            if (!user[4].isEmpty()) addText(card, getString(R.string.username_value, user[4]), 13, false);
            Button delete = new Button(this);
            delete.setText(R.string.admin_delete_user);
            delete.setAllCaps(false);
            delete.setOnClickListener(view -> confirmDelete(user));
            card.addView(delete);
            Button resetPassword = new Button(this);
            resetPassword.setText(R.string.admin_reset_password);
            resetPassword.setAllCaps(false);
            resetPassword.setOnClickListener(view -> changePassword(user));
            card.addView(resetPassword);
            usersList.addView(card);
        }
        emptyMessage.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void changePassword(String[] user) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        form.setPadding(padding, padding, padding, 0);
        android.widget.EditText password = new android.widget.EditText(this);
        password.setHint(R.string.new_password);
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        android.widget.EditText confirm = new android.widget.EditText(this);
        confirm.setHint(R.string.confirm_new_password);
        confirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password);
        form.addView(confirm);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.admin_reset_password_for, user[1]))
                .setView(form).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_reset_password, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    if (!password.getText().toString().equals(confirm.getText().toString())) {
                        confirm.setError(getString(R.string.passwords_do_not_match));
                        return;
                    }
                    if (!authStore.resetStudentPassword(user[0], password.getText().toString())) {
                        password.setError(getString(R.string.error_password_policy));
                        return;
                    }
                    Toast.makeText(this, R.string.admin_password_reset, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void confirmDelete(String[] user) {
        new AlertDialog.Builder(this).setTitle(R.string.admin_delete_user_title)
                .setMessage(getString(R.string.admin_delete_user_confirm, user[1]))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_delete_user, (dialog, which) -> {
                    if (!authStore.removeStudentAccount(user[0])) {
                        Toast.makeText(this, R.string.admin_delete_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    database.deleteUserData(user[0]);
                    Toast.makeText(this, R.string.admin_delete_success, Toast.LENGTH_SHORT).show();
                    refreshUsers();
                }).show();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(15);
        card.setPadding(padding, padding, padding, padding);
        card.setBackgroundResource(R.drawable.bg_home_surface);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private void addText(LinearLayout card, String text, int size, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(getColor(R.color.text_primary));
        label.setPadding(0, 0, 0, dp(4));
        if (bold) label.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(label);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(getColor(R.color.home_background));
        getWindow().setNavigationBarColor(getColor(R.color.home_background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
