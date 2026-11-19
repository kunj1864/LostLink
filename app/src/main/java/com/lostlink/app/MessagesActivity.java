package com.lostlink.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.ChatMessage;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;
import com.lostlink.app.data.LocalNotifier;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MessagesActivity extends Activity {
    private AuthStore authStore;
    private LostLinkDatabase database;
    private LostItem item;
    private String recipientEmail;
    private String recipientName;
    private LinearLayout messagesList;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        configureSystemBars();
        authStore = new AuthStore(this);
        database = new LostLinkDatabase(this);
        item = database.getItem(getIntent().getLongExtra("item_id", -1L));
        recipientEmail = getIntent().getStringExtra("recipient_email");
        recipientName = getIntent().getStringExtra("recipient_name");
        if (item == null || recipientEmail == null || recipientEmail.isEmpty()
                || recipientEmail.equals(authStore.getEmail())) {
            Toast.makeText(this, R.string.error_conversation_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (recipientName == null || recipientName.trim().isEmpty()) recipientName = recipientEmail;

        ((TextView) findViewById(R.id.messageTitle)).setText(
                getString(R.string.message_title_format, recipientName));
        ((TextView) findViewById(R.id.messageItemName)).setText(item.name);
        messagesList = findViewById(R.id.messagesList);
        messageInput = findViewById(R.id.messageInput);
        findViewById(R.id.messagesBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.sendMessageButton).setOnClickListener(view -> sendMessage());
        refreshMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null && item != null) refreshMessages();
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            messageInput.setError(getString(R.string.error_empty_message));
            return;
        }
        try {
            boolean sent = database.sendMessage(item.id, authStore.getEmail(), recipientEmail, text);
            if (!sent) {
                Toast.makeText(this, R.string.error_conversation_unavailable, Toast.LENGTH_LONG).show();
                return;
            }
            LocalNotifier.showForSignedInUser(this, recipientEmail,
                    getString(R.string.new_message_title),
                    getString(R.string.new_message_notification, item.name), item.id);
            messageInput.setText("");
            refreshMessages();
        } catch (android.database.sqlite.SQLiteException exception) {
            Toast.makeText(this, R.string.error_send_message, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshMessages() {
        ArrayList<ChatMessage> messages = database.findMessages(item.id,
                authStore.getEmail(), recipientEmail);
        messagesList.removeAllViews();
        for (ChatMessage message : messages) {
            boolean sentByMe = message.senderEmail.equals(authStore.getEmail());
            LinearLayout bubble = new LinearLayout(this);
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(dp(13), dp(10), dp(13), dp(9));
            bubble.setBackgroundResource(sentByMe ? R.drawable.bg_primary_button : R.drawable.bg_card);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.78f),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = sentByMe ? android.view.Gravity.END : android.view.Gravity.START;
            params.setMargins(0, 0, 0, dp(10));
            bubble.setLayoutParams(params);

            TextView body = new TextView(this);
            body.setText(message.message);
            body.setTextColor(sentByMe ? getColor(android.R.color.white) : getColor(R.color.text_primary));
            body.setTextSize(14);
            bubble.addView(body);
            TextView time = new TextView(this);
            time.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(message.createdAt)));
            time.setTextColor(sentByMe ? 0xFFDCE7FF : getColor(R.color.text_secondary));
            time.setTextSize(10);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            timeParams.gravity = android.view.Gravity.END;
            time.setLayoutParams(timeParams);
            bubble.addView(time);
            messagesList.addView(bubble);
        }
        findViewById(R.id.messagesScroll).post(() ->
                ((android.widget.ScrollView) findViewById(R.id.messagesScroll)).fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
