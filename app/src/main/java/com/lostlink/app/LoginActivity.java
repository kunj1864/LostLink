package com.lostlink.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;

public class LoginActivity extends Activity {
    private EditText emailInput;
    private EditText passwordInput;
    private TextView errorMessage;
    private AuthStore authStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(R.layout.activity_login);

        authStore = new AuthStore(this);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        errorMessage = findViewById(R.id.loginError);
        Button loginButton = findViewById(R.id.loginButton);
        TextView registerLink = findViewById(R.id.registerLink);

        loginButton.setOnClickListener(view -> attemptLogin());
        findViewById(R.id.forgotPasswordLink).setOnClickListener(view ->
                new AlertDialog.Builder(this).setTitle(R.string.forgot_password_title)
                        .setMessage(R.string.offline_forgot_password_help)
                        .setPositiveButton(android.R.string.ok, null).show());
        registerLink.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        errorMessage.setVisibility(View.GONE);
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email));
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError(getString(R.string.error_password_required));
            passwordInput.requestFocus();
            return;
        }
        if (!authStore.login(email, password)) {
            showError(getString(R.string.error_login_failed));
            return;
        }

        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }
}
