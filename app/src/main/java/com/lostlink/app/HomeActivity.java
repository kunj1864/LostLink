package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;

import java.util.ArrayList;

public final class HomeActivity extends Activity {
    private LostLinkDatabase database;
    private EditText itemSearchInput;
    private EditText locationSearchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.home_background));
        getWindow().setNavigationBarColor(getColor(R.color.home_background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setContentView(R.layout.activity_home);

        database = new LostLinkDatabase(this);
        itemSearchInput = findViewById(R.id.homeItemSearchInput);
        locationSearchInput = findViewById(R.id.homeLocationSearchInput);

        findViewById(R.id.homeLoginButton).setOnClickListener(view -> openAccount());
        findViewById(R.id.homeRegisterButton).setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.homeReportLostButton).setOnClickListener(view -> openReport("LOST"));
        findViewById(R.id.homeReportFoundButton).setOnClickListener(view -> openReport("FOUND"));
        findViewById(R.id.homeSearchButton).setOnClickListener(view -> searchItems());
        findViewById(R.id.homeViewAllButton).setOnClickListener(view -> openItems(null, null));
        findViewById(R.id.homeAboutButton).setOnClickListener(view ->
                startActivity(new Intent(this, HelpActivity.class)));
        findViewById(R.id.homeContactButton).setOnClickListener(view -> {
            Intent intent = new Intent(this, HelpActivity.class);
            intent.putExtra("show_contact", true);
            startActivity(intent);
        });
        populateRecentItems();
    }

    private void openAccount() {
        Class<?> destination = new AuthStore(this).isLoggedIn()
                ? DashboardActivity.class : LoginActivity.class;
        startActivity(new Intent(this, destination));
    }

    private void openReport(String type) {
        if (!new AuthStore(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        Intent intent = new Intent(this, ReportItemActivity.class);
        intent.putExtra("item_type", type);
        startActivity(intent);
    }

    private void searchItems() {
        String query = itemSearchInput.getText().toString().trim();
        if (query.isEmpty()) {
            itemSearchInput.setError(getString(R.string.home_search_required));
            itemSearchInput.requestFocus();
            return;
        }
        openItems(query, locationSearchInput.getText().toString().trim());
    }

    private void openItems(String query, String location) {
        Intent intent = new Intent(this, ItemsActivity.class);
        if (query != null) intent.putExtra("search_query", query);
        if (location != null) intent.putExtra("location_query", location);
        startActivity(intent);
    }

    private void populateRecentItems() {
        LinearLayout recentItems = findViewById(R.id.homeRecentItemsList);
        ArrayList<LostItem> items = database.findItems(null, null, null, null, false);
        if (items.isEmpty()) {
            findViewById(R.id.homeNoRecentItems).setVisibility(View.VISIBLE);
            return;
        }
        for (int index = 0; index < Math.min(items.size(), 3); index++) {
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
                } catch (SecurityException | IllegalArgumentException exception) {
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
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
