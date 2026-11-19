package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;

public class RegisterActivity extends Activity {
    private EditText nameInput;
    private EditText emailInput;
    private EditText usernameInput;
    private EditText mobileInput;
    private EditText addressInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private TextView errorMessage;
    private AuthStore authStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(R.layout.activity_register);

        authStore = new AuthStore(this);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        usernameInput = findViewById(R.id.usernameInput);
        mobileInput = findViewById(R.id.mobileInput);
        addressInput = findViewById(R.id.addressInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        errorMessage = findViewById(R.id.registerError);

        Button registerButton = findViewById(R.id.registerButton);
        TextView loginLink = findViewById(R.id.loginLink);
        TextView backLink = findViewById(R.id.backLink);
        registerButton.setOnClickListener(view -> attemptRegistration());
        loginLink.setOnClickListener(view -> finish());
        backLink.setOnClickListener(view -> finish());
    }

    private void attemptRegistration() {
        errorMessage.setVisibility(View.GONE);
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String mobile = mobileInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmation = confirmPasswordInput.getText().toString();

        if (name.length() < 2) {
            showError(getString(R.string.error_name));
            nameInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email));
            emailInput.requestFocus();
            return;
        }
        if (!password.matches("(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}")) {
            showError(getString(R.string.error_password_policy));
            passwordInput.requestFocus();
            return;
        }
        if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            showError(getString(R.string.error_username));
            usernameInput.requestFocus();
            return;
        }
        if (!mobile.matches("[0-9]{10}")) {
            showError(getString(R.string.error_mobile));
            mobileInput.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            showError(getString(R.string.error_address));
            addressInput.requestFocus();
            return;
        }
        if (!password.equals(confirmation)) {
            showError(getString(R.string.error_password_mismatch));
            confirmPasswordInput.requestFocus();
            return;
        }
        if (!authStore.register(name, email, password, username, mobile, address)) {
            showError(getString(R.string.error_account_exists));
            return;
        }
        com.lostlink.app.data.LostLinkDatabase database =
                new com.lostlink.app.data.LostLinkDatabase(this);
        try {
            database.saveUserProfile(email.toLowerCase(java.util.Locale.ROOT), name, mobile,
                    address, username);
        } finally {
            database.close();
        }

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }
}
