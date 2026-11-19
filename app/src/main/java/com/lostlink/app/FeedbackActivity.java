package com.lostlink.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.FeedbackEntry;
import com.lostlink.app.data.LostLinkDatabase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public final class FeedbackActivity extends Activity {
    private AuthStore authStore;
    private LostLinkDatabase database;
    private LinearLayout list;
    private EditText nameInput;
    private EditText emailInput;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        configureSystemBars();
        authStore = new AuthStore(this);
        if (!authStore.isLoggedIn()) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }
        database = new LostLinkDatabase(this);
        list = findViewById(R.id.feedbackList);
        findViewById(R.id.feedbackBackButton).setOnClickListener(view -> finish());
        nameInput = findViewById(R.id.feedbackNameInput);
        emailInput = findViewById(R.id.feedbackEmailInput);
        messageInput = findViewById(R.id.feedbackMessageInput);
        nameInput.setText(authStore.getDisplayName());
        emailInput.setText(authStore.getEmail());
        findViewById(R.id.feedbackSubmitButton).setOnClickListener(view -> submitFeedback());
        boolean admin = authStore.isAdmin();
        findViewById(R.id.feedbackForm).setVisibility(admin ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.feedbackTitle)).setText(
                admin ? R.string.admin_feedback_title : R.string.feedback_title);
        refreshFeedback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshFeedback();
    }

    private void submitFeedback() {
        String email = emailInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.error_email));
            return;
        }
        if (message.isEmpty() || message.length() > 2000) {
            messageInput.setError(getString(R.string.feedback_message_error));
            return;
        }
        long id = database.submitFeedback(authStore.getEmail(),
                nameInput.getText().toString(), email, message);
        if (id < 0) {
            Toast.makeText(this, R.string.feedback_submit_failed, Toast.LENGTH_LONG).show();
            return;
        }
        messageInput.setText("");
        Toast.makeText(this, R.string.feedback_submit_success, Toast.LENGTH_SHORT).show();
        refreshFeedback();
    }

    private void refreshFeedback() {
        ArrayList<FeedbackEntry> entries = database.findFeedback(
                authStore.isAdmin() ? null : authStore.getEmail());
        list.removeAllViews();
        for (FeedbackEntry entry : entries) {
            LinearLayout card = card();
            addText(card, entry.name + " · " + DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(entry.createdAt)), 14, true);
            if (authStore.isAdmin()) addText(card, entry.email, 12, false);
            addText(card, entry.message, 14, false);
            if (authStore.isAdmin()) {
                EditText reply = new EditText(this);
                reply.setHint(R.string.feedback_reply_hint);
                reply.setMinLines(2);
                reply.setGravity(android.view.Gravity.TOP);
                if (entry.adminReply != null) reply.setText(entry.adminReply);
                card.addView(reply);
                Button send = new Button(this);
                send.setText(R.string.feedback_reply_button);
                send.setAllCaps(false);
                send.setOnClickListener(view -> {
                    if (!database.replyToFeedback(entry.id, reply.getText().toString())) {
                        Toast.makeText(this, R.string.feedback_reply_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(this, R.string.feedback_reply_saved, Toast.LENGTH_SHORT).show();
                    refreshFeedback();
                });
                card.addView(send);
            } else if (entry.adminReply != null && !entry.adminReply.trim().isEmpty()) {
                addText(card, getString(R.string.feedback_admin_reply, entry.adminReply), 13, true);
            } else {
                addText(card, getString(R.string.feedback_waiting_reply), 12, false);
            }
            list.addView(card);
        }
        findViewById(R.id.feedbackEmpty).setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(14);
        card.setPadding(padding, padding, padding, padding);
        card.setBackgroundResource(R.drawable.bg_home_surface);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    private void addText(LinearLayout card, String value, int size, boolean bold) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(getColor(R.color.text_primary));
        label.setPadding(0, 0, 0, dp(6));
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
