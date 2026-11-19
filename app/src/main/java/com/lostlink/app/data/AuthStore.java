package com.lostlink.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

public final class AuthStore {
    private static final String PREFS_NAME = "lostlink_auth";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_SESSION_EMAIL = "session_email";
    private static final String KEY_SESSION_NAME = "session_name";
    private static final String DEMO_EMAIL = "student@lostlink.edu";
    private static final String DEMO_PASSWORD = "lostlink123";
    private static final String ADMIN_EMAIL = "admin@lostlink.edu";
    private static final String ADMIN_PASSWORD = "adminlink123";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");

    private final SharedPreferences preferences;

    public AuthStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        seedDemoAccount();
    }

    public boolean login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
                || password == null || password.isEmpty()) {
            return false;
        }

        String savedHash = preferences.getString(accountPasswordKey(normalizedEmail), null);
        String savedName = preferences.getString(accountNameKey(normalizedEmail), null);
        if (savedHash == null || savedName == null || !savedHash.equals(hash(password))) {
            return false;
        }

        preferences.edit()
                .putString(KEY_SESSION_EMAIL, normalizedEmail)
                .putString(KEY_SESSION_NAME, savedName)
                .apply();
        return true;
    }

    public boolean register(String name, String email, String password) {
        return register(name, email, password, "", "", "");
    }

    public boolean register(String name, String email, String password, String username,
                            String mobile, String address) {
        String normalizedEmail = normalizeEmail(email);
        if (name == null || name.trim().length() < 2
                || !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
                || !isPasswordStrong(password)
                || username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()
                || usernameExists(username.trim(), normalizedEmail)
                || mobile == null || !MOBILE_PATTERN.matcher(mobile.trim()).matches()
                || address == null || address.trim().isEmpty()
                || preferences.contains(accountPasswordKey(normalizedEmail))) {
            return false;
        }

        preferences.edit()
                .putString(accountNameKey(normalizedEmail), name.trim())
                .putString(accountPasswordKey(normalizedEmail), hash(password))
                .putString(accountUsernameKey(normalizedEmail), username.trim())
                .putString(accountMobileKey(normalizedEmail), mobile.trim())
                .putString(accountAddressKey(normalizedEmail), address.trim())
                .putString(KEY_SESSION_EMAIL, normalizedEmail)
                .putString(KEY_SESSION_NAME, name.trim())
                .apply();
        return true;
    }

    public String getUsername() {
        return preferences.getString(accountUsernameKey(getEmail()), "");
    }

    public String getMobile() {
        return preferences.getString(accountMobileKey(getEmail()), "");
    }

    public String getAddress() {
        return preferences.getString(accountAddressKey(getEmail()), "");
    }

    public boolean updateProfile(String name, String username, String mobile, String address) {
        String email = getEmail();
        if (!isLoggedIn() || name == null || name.trim().length() < 2
                || username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()
                || usernameExists(username.trim(), email)
                || mobile == null || !MOBILE_PATTERN.matcher(mobile.trim()).matches()
                || address == null || address.trim().isEmpty()) {
            return false;
        }
        preferences.edit().putString(accountNameKey(email), name.trim())
                .putString(accountUsernameKey(email), username.trim())
                .putString(accountMobileKey(email), mobile.trim())
                .putString(accountAddressKey(email), address.trim())
                .putString(KEY_SESSION_NAME, name.trim()).apply();
        return true;
    }

    public boolean changePassword(String currentPassword, String newPassword) {
        String email = getEmail();
        String savedHash = preferences.getString(accountPasswordKey(email), null);
        if (savedHash == null || currentPassword == null || !savedHash.equals(hash(currentPassword))
                || !isPasswordStrong(newPassword)) {
            return false;
        }
        preferences.edit().putString(accountPasswordKey(email), hash(newPassword)).apply();
        return true;
    }

    public ArrayList<String[]> getStudentAccounts() {
        ArrayList<String[]> accounts = new ArrayList<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("account_password_")) continue;
            String email = key.substring("account_password_".length());
            if (ADMIN_EMAIL.equalsIgnoreCase(email)) continue;
            accounts.add(new String[]{email,
                    preferences.getString(accountNameKey(email), email),
                    preferences.getString(accountMobileKey(email), ""),
                    preferences.getString(accountAddressKey(email), ""),
                    preferences.getString(accountUsernameKey(email), "")});
        }
        Collections.sort(accounts, Comparator.comparing(account -> account[1].toLowerCase(Locale.ROOT)));
        return accounts;
    }

    public boolean removeStudentAccount(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty() || ADMIN_EMAIL.equals(normalizedEmail)
                || !preferences.contains(accountPasswordKey(normalizedEmail))) {
            return false;
        }
        preferences.edit().remove(accountNameKey(normalizedEmail))
                .remove(accountPasswordKey(normalizedEmail))
                .remove(accountUsernameKey(normalizedEmail))
                .remove(accountMobileKey(normalizedEmail))
                .remove(accountAddressKey(normalizedEmail)).apply();
        return true;
    }

    public boolean resetStudentPassword(String email, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (!isAdmin() || normalizedEmail.isEmpty() || ADMIN_EMAIL.equals(normalizedEmail)
                || !preferences.contains(accountPasswordKey(normalizedEmail))
                || !isPasswordStrong(newPassword)) {
            return false;
        }
        preferences.edit().putString(accountPasswordKey(normalizedEmail), hash(newPassword)).apply();
        return true;
    }

    public boolean isLoggedIn() {
        return preferences.getString(KEY_SESSION_EMAIL, null) != null;
    }

    public String getDisplayName() {
        String name = preferences.getString(KEY_SESSION_NAME, null);
        return name == null ? "Student" : name;
    }

    public String getEmail() {
        String email = preferences.getString(KEY_SESSION_EMAIL, null);
        return email == null ? "" : email;
    }

    public boolean isAdmin() {
        return ADMIN_EMAIL.equals(normalizeEmail(getEmail()));
    }

    public void logout() {
        preferences.edit()
                .remove(KEY_SESSION_EMAIL)
                .remove(KEY_SESSION_NAME)
                .apply();
    }

    private void seedDemoAccount() {
        SharedPreferences.Editor editor = preferences.edit();
        if (!preferences.getBoolean(KEY_INITIALIZED, false)) {
            editor.putString(accountNameKey(DEMO_EMAIL), "Demo Student")
                    .putString(accountPasswordKey(DEMO_EMAIL), hash(DEMO_PASSWORD))
                    .putBoolean(KEY_INITIALIZED, true);
        }
        if (!preferences.contains(accountPasswordKey(ADMIN_EMAIL))) {
            editor.putString(accountNameKey(ADMIN_EMAIL), "LostLink Admin")
                    .putString(accountPasswordKey(ADMIN_EMAIL), hash(ADMIN_PASSWORD));
        }
        editor.apply();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String accountNameKey(String email) {
        return "account_name_" + email;
    }

    private static String accountPasswordKey(String email) {
        return "account_password_" + email;
    }

    private static String accountUsernameKey(String email) {
        return "account_username_" + email;
    }

    private static String accountMobileKey(String email) {
        return "account_mobile_" + email;
    }

    private static String accountAddressKey(String email) {
        return "account_address_" + email;
    }

    private boolean usernameExists(String username, String currentEmail) {
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getKey().startsWith("account_username_")
                    && username.equalsIgnoreCase(String.valueOf(entry.getValue()))
                    && !entry.getKey().equals(accountUsernameKey(currentEmail))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*") && password.matches(".*[^A-Za-z0-9].*");
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
