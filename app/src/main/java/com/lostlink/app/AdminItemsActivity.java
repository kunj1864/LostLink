package com.lostlink.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;
import com.lostlink.app.data.MatchNotifier;

import java.util.ArrayList;

public final class AdminItemsActivity extends Activity {
    private LostLinkDatabase database;
    private LinearLayout list;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);
        configureSystemBars();
        if (!new AuthStore(this).isAdmin()) {
            Toast.makeText(this, R.string.admin_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        database = new LostLinkDatabase(this);
        ((TextView) findViewById(R.id.adminListTitle)).setText(R.string.admin_items_title);
        ((TextView) findViewById(R.id.adminListEmpty)).setText(R.string.admin_items_empty);
        list = findViewById(R.id.adminListContent);
        emptyMessage = findViewById(R.id.adminListEmpty);
        findViewById(R.id.adminListBackButton).setOnClickListener(view -> finish());
        refreshItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshItems();
    }

    private void refreshItems() {
        ArrayList<LostItem> items = database.findItems(null, null, null, null, true);
        list.removeAllViews();
        for (LostItem item : items) {
            View card = getLayoutInflater().inflate(R.layout.row_admin_review, list, false);
            ((TextView) card.findViewById(R.id.reviewItemName)).setText(item.name);
            ((TextView) card.findViewById(R.id.reviewItemMeta)).setText(
                    getString(R.string.admin_item_meta, item.type, item.location, item.status,
                            item.reviewStatus));
            ((TextView) card.findViewById(R.id.reviewItemReporter)).setText(
                    getString(R.string.details_reporter, item.reporterName));
            ((TextView) card.findViewById(R.id.reviewItemDescription)).setText(item.description);
            TextView evidence = card.findViewById(R.id.reviewItemEvidence);
            String evidenceText = "LOST".equals(item.type) ? item.ownershipProof : item.findingEvidence;
            if (evidenceText != null && !evidenceText.trim().isEmpty()) {
                evidence.setText(getString(R.string.admin_report_evidence, evidenceText));
                evidence.setVisibility(View.VISIBLE);
            }
            ImageView photo = card.findViewById(R.id.reviewItemPhoto);
            if (item.photoUri != null) {
                try {
                    photo.setImageURI(android.net.Uri.parse(item.photoUri));
                } catch (SecurityException | IllegalArgumentException exception) {
                    photo.setImageResource(R.drawable.ic_lostlink);
                }
            }
            Button approve = card.findViewById(R.id.adminApproveButton);
            Button reject = card.findViewById(R.id.adminRejectButton);
            if ("Pending".equals(item.reviewStatus)) {
                approve.setText(R.string.admin_approve);
                reject.setText(R.string.admin_reject);
                approve.setVisibility(View.VISIBLE);
                reject.setVisibility(View.VISIBLE);
                approve.setOnClickListener(view -> review(item, "Approved"));
                reject.setOnClickListener(view -> review(item, "Rejected"));
            } else {
                approve.setText("Resolved".equals(item.status)
                        ? R.string.admin_reopen_item : R.string.admin_resolve_item);
                reject.setText(R.string.admin_delete_item);
                approve.setVisibility(View.VISIBLE);
                reject.setVisibility(View.VISIBLE);
                approve.setOnClickListener(view -> updateStatus(item,
                        "Resolved".equals(item.status) ? "Pending" : "Resolved"));
                reject.setOnClickListener(view -> confirmDelete(item));
            }
            card.setOnClickListener(view -> {
                Intent intent = new Intent(this, ItemDetailsActivity.class);
                intent.putExtra("item_id", item.id);
                startActivity(intent);
            });
            list.addView(card);
        }
        emptyMessage.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void review(LostItem item, String status) {
        if (!database.updateReviewStatus(item.id, status)) {
            Toast.makeText(this, R.string.admin_review_failed, Toast.LENGTH_LONG).show();
            refreshItems();
            return;
        }
        if ("Approved".equals(status)) {
            LostItem approvedItem = database.getItem(item.id);
            if (approvedItem != null) MatchNotifier.notifyMatches(this, database, approvedItem);
        }
        refreshItems();
    }

    private void updateStatus(LostItem item, String status) {
        if (!database.updateItemStatusAsAdmin(item.id, status)) {
            Toast.makeText(this, R.string.admin_item_update_failed, Toast.LENGTH_LONG).show();
            return;
        }
        refreshItems();
    }

    private void confirmDelete(LostItem item) {
        new AlertDialog.Builder(this).setTitle(R.string.admin_delete_item_title)
                .setMessage(getString(R.string.admin_delete_item_confirm, item.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.admin_delete_item, (dialog, which) -> {
                    if (!database.deleteItemAsAdmin(item.id)) {
                        Toast.makeText(this, R.string.admin_item_update_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    refreshItems();
                }).show();
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
