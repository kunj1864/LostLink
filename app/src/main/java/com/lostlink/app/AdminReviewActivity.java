package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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

public class AdminReviewActivity extends Activity {
    private AuthStore authStore;
    private LostLinkDatabase database;
    private LinearLayout reviewList;
    private TextView emptyMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_review);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        authStore = new AuthStore(this);
        if (!authStore.isAdmin()) {
            Toast.makeText(this, R.string.admin_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        database = new LostLinkDatabase(this);
        reviewList = findViewById(R.id.adminReviewList);
        emptyMessage = findViewById(R.id.adminReviewEmpty);
        findViewById(R.id.adminReviewBackButton).setOnClickListener(view -> finish());
        refreshReviewQueue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshReviewQueue();
    }

    private void refreshReviewQueue() {
        ArrayList<LostItem> items = database.findItemsForReview();
        reviewList.removeAllViews();
        for (LostItem item : items) {
            View card = getLayoutInflater().inflate(R.layout.row_admin_review, reviewList, false);
            ((TextView) card.findViewById(R.id.reviewItemName)).setText(item.name);
            ((TextView) card.findViewById(R.id.reviewItemMeta)).setText(
                    getString(R.string.item_row_meta, item.type, item.location));
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
                    photo.setImageURI(Uri.parse(item.photoUri));
                } catch (SecurityException | IllegalArgumentException exception) {
                    photo.setImageResource(R.drawable.ic_lostlink);
                }
            }
            Button approve = card.findViewById(R.id.adminApproveButton);
            Button reject = card.findViewById(R.id.adminRejectButton);
            approve.setOnClickListener(view -> reviewItem(item, "Approved"));
            reject.setOnClickListener(view -> reviewItem(item, "Rejected"));
            card.setOnClickListener(view -> {
                Intent intent = new Intent(this, ItemDetailsActivity.class);
                intent.putExtra("item_id", item.id);
                startActivity(intent);
            });
            reviewList.addView(card);
        }
        emptyMessage.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void reviewItem(LostItem item, String status) {
        if (!authStore.isAdmin()) {
            Toast.makeText(this, R.string.admin_access_denied, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!database.updateReviewStatus(item.id, status)) {
            Toast.makeText(this, R.string.admin_review_failed, Toast.LENGTH_LONG).show();
            refreshReviewQueue();
            return;
        }
        if ("Approved".equals(status)) {
            LostItem approvedItem = database.getItem(item.id);
            if (approvedItem != null) {
                MatchNotifier.notifyMatches(this, database, approvedItem);
            }
        }
        Toast.makeText(this, R.string.admin_review_saved, Toast.LENGTH_SHORT).show();
        refreshReviewQueue();
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
