package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostLinkDatabase;

public final class AdminConsoleActivity extends Activity {
    private LostLinkDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_console);
        AuthStore authStore = new AuthStore(this);
        if (!authStore.isAdmin()) {
            Toast.makeText(this, R.string.admin_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        getWindow().setStatusBarColor(getColor(R.color.home_background));
        getWindow().setNavigationBarColor(getColor(R.color.home_background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        database = new LostLinkDatabase(this);
        findViewById(R.id.adminConsoleBackButton).setOnClickListener(view -> finish());
        bind(R.id.adminConsoleReviewButton, AdminReviewActivity.class);
        bind(R.id.adminConsoleUsersButton, AdminUsersActivity.class);
        bind(R.id.adminConsoleItemsButton, AdminItemsActivity.class);
        bind(R.id.adminConsoleClaimsButton, AdminClaimsActivity.class);
        bind(R.id.adminConsoleFeedbackButton, FeedbackActivity.class);
        bind(R.id.adminConsoleMessagesButton, SystemMessagesActivity.class);
        refreshCounts();
    }

    private void bind(int id, Class<?> destination) {
        findViewById(id).setOnClickListener(view -> startActivity(new Intent(this, destination)));
    }

    private void refreshCounts() {
        ((TextView) findViewById(R.id.adminConsoleUsersCount))
                .setText(getString(R.string.admin_count_users, database.countUsers()));
        ((TextView) findViewById(R.id.adminConsolePendingCount))
                .setText(getString(R.string.admin_count_pending, database.findItemsForReview().size()));
        ((TextView) findViewById(R.id.adminConsoleClaimsCount))
                .setText(getString(R.string.admin_count_claims, database.findAllClaims().size()));
        ((TextView) findViewById(R.id.adminConsoleFeedbackCount))
                .setText(getString(R.string.admin_count_feedback, database.findFeedback(null).size()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshCounts();
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
