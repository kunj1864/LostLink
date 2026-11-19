package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;

import java.util.ArrayList;

public class DashboardActivity extends Activity {
    private AuthStore authStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(R.layout.activity_dashboard);

        authStore = new AuthStore(this);
        if (!authStore.isLoggedIn()) {
            openLogin();
            return;
        }

        TextView greeting = findViewById(R.id.greetingText);
        greeting.setText(getString(R.string.dashboard_greeting, firstName(authStore.getDisplayName())));
        configureActions();
        populateDashboard();
        TextView logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(view -> {
            authStore.logout();
            openLogin();
        });
    }

    private void configureActions() {
        findViewById(R.id.reportLostAction).setOnClickListener(view -> openReport("LOST"));
        findViewById(R.id.reportFoundAction).setOnClickListener(view -> openReport("FOUND"));
        findViewById(R.id.browseItemsAction).setOnClickListener(view ->
                startActivity(new Intent(this, ItemsActivity.class)));
        findViewById(R.id.myProfileAction).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.claimsAction).setOnClickListener(view ->
                startActivity(new Intent(this, ClaimsActivity.class)));
        findViewById(R.id.notificationsAction).setOnClickListener(view ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.viewAllReportsAction).setOnClickListener(view ->
                startActivity(new Intent(this, ItemsActivity.class)));
    }

    private void openReport(String type) {
        Intent intent = new Intent(this, ReportItemActivity.class);
        intent.putExtra("item_type", type);
        startActivity(intent);
    }

    private void populateDashboard() {
        LostLinkDatabase database = new LostLinkDatabase(this);
        try {
            database.saveUser(authStore.getEmail(), authStore.getDisplayName());
            ((TextView) findViewById(R.id.myReportCount)).setText(
                    String.valueOf(database.countItems(authStore.getEmail(), null)));
            ((TextView) findViewById(R.id.pendingReportCount)).setText(
                    String.valueOf(database.countItems(authStore.getEmail(), "Pending")));
            ((TextView) findViewById(R.id.claimsAction)).setText(
                    getString(R.string.claims_action_count, database.countClaims(
                            authStore.getEmail(), false, null)
                            + database.countClaims(authStore.getEmail(), true, null)));
            ((TextView) findViewById(R.id.notificationsAction)).setText(
                    getString(R.string.notifications_action_count,
                            database.countUnreadNotifications(authStore.getEmail())));
            LinearLayout recentItems = findViewById(R.id.recentItemsList);
            recentItems.removeAllViews();
            ArrayList<LostItem> items = database.findItems(null, null, null, null,
                    authStore.isAdmin());
            int count = Math.min(items.size(), 3);
            for (int index = 0; index < count; index++) {
                LostItem item = items.get(index);
                View row = getLayoutInflater().inflate(R.layout.row_item, recentItems, false);
                ((TextView) row.findViewById(R.id.rowItemName)).setText(item.name);
                ((TextView) row.findViewById(R.id.rowItemMeta)).setText(
                        getString(R.string.item_row_meta, item.category, item.location));
                ((TextView) row.findViewById(R.id.rowItemDescription)).setText(item.description);
                ((TextView) row.findViewById(R.id.rowItemType)).setText(
                        "LOST".equals(item.type) ? R.string.lost_badge : R.string.found_badge);
                ImageView image = row.findViewById(R.id.rowItemPhoto);
                if (item.photoUri != null) {
                    try {
                        image.setImageURI(android.net.Uri.parse(item.photoUri));
                    } catch (SecurityException exception) {
                        image.setImageResource(R.drawable.ic_lostlink);
                    }
                }
                row.setOnClickListener(view -> {
                    Intent intent = new Intent(this, ItemDetailsActivity.class);
                    intent.putExtra("item_id", item.id);
                    startActivity(intent);
                });
                recentItems.addView(row);
            }
        } finally {
            database.close();
        }
    }

    private String firstName(String fullName) {
        String trimmedName = fullName == null ? "" : fullName.trim();
        int separator = trimmedName.indexOf(' ');
        return separator > 0 ? trimmedName.substring(0, separator) : trimmedName;
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authStore != null && authStore.isLoggedIn()) {
            populateDashboard();
        }
    }
}
