package com.lostlink.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.ChatMessage;
import com.lostlink.app.data.LostLinkDatabase;
import com.lostlink.app.data.LocalNotifier;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public final class SystemMessagesActivity extends Activity {
    private AuthStore authStore;
    private LostLinkDatabase database;
    private Spinner userSpinner;
    private LinearLayout list;
    private EditText messageInput;
    private ArrayList<String[]> accounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_messages);
        configureSystemBars();
        authStore = new AuthStore(this);
        if (!authStore.isLoggedIn()) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }
        database = new LostLinkDatabase(this);
        userSpinner = findViewById(R.id.systemMessageUserSpinner);
        list = findViewById(R.id.systemMessagesList);
        messageInput = findViewById(R.id.systemMessageInput);
        findViewById(R.id.systemMessagesBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.systemMessageSendButton).setOnClickListener(view -> sendMessage());
        boolean admin = authStore.isAdmin();
        userSpinner.setVisibility(admin ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.systemMessagesTitle)).setText(
                admin ? R.string.admin_manage_messages : R.string.system_messages_title);
        if (admin) {
            accounts = authStore.getStudentAccounts();
            ArrayList<String> labels = new ArrayList<>();
            for (String[] account : accounts) labels.add(account[1] + " · " + account[0]);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            userSpinner.setAdapter(adapter);
            userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    refreshMessages();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
        }
        refreshMessages();
    }

    private String selectedUserEmail() {
        if (!authStore.isAdmin()) return "admin@lostlink.edu";
        int position = userSpinner.getSelectedItemPosition();
        return accounts == null || position < 0 || position >= accounts.size()
                ? null : accounts.get(position)[0];
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        String recipient = selectedUserEmail();
        if (text.isEmpty()) {
            messageInput.setError(getString(R.string.error_empty_message));
            return;
        }
        if (recipient == null) {
            Toast.makeText(this, R.string.admin_no_users, Toast.LENGTH_LONG).show();
            return;
        }
        String sender = authStore.getEmail();
        if (!database.sendSystemMessage(sender, recipient, text)) {
            Toast.makeText(this, R.string.error_send_message, Toast.LENGTH_LONG).show();
            return;
        }
        LocalNotifier.showForSignedInUser(this, recipient,
                getString(R.string.new_message_title), getString(R.string.system_message_notification), -1L);
        messageInput.setText("");
        refreshMessages();
    }

    private void refreshMessages() {
        if (list == null || database == null || !authStore.isLoggedIn()) return;
        String other = selectedUserEmail();
        if (other == null) {
            findViewById(R.id.systemMessagesEmpty).setVisibility(View.VISIBLE);
            return;
        }
        ArrayList<ChatMessage> messages = database.findSystemMessages(authStore.getEmail(), other);
        list.removeAllViews();
        for (ChatMessage message : messages) {
            boolean sent = authStore.getEmail().equals(message.senderEmail);
            LinearLayout bubble = new LinearLayout(this);
            bubble.setOrientation(LinearLayout.VERTICAL);
            int padding = dp(12);
            bubble.setPadding(padding, padding, padding, padding);
            bubble.setBackgroundResource(sent ? R.drawable.bg_primary_button : R.drawable.bg_home_surface);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.82f),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = sent ? android.view.Gravity.END : android.view.Gravity.START;
            params.bottomMargin = dp(8);
            bubble.setLayoutParams(params);
            TextView body = new TextView(this);
            body.setText(message.message);
            body.setTextColor(getColor(sent ? android.R.color.white : R.color.text_primary));
            body.setTextSize(14);
            bubble.addView(body);
            TextView time = new TextView(this);
            time.setText(DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(new Date(message.createdAt)));
            time.setTextColor(getColor(sent ? android.R.color.white : R.color.text_secondary));
            time.setTextSize(10);
            time.setGravity(android.view.Gravity.END);
            bubble.addView(time);
            list.addView(bubble);
        }
        findViewById(R.id.systemMessagesEmpty).setVisibility(
                messages.isEmpty() ? View.VISIBLE : View.GONE);
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
