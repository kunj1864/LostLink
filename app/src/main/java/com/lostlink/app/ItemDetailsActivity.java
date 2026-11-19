package com.lostlink.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;
import com.lostlink.app.data.LocalNotifier;
import com.lostlink.app.data.MatchNotifier;

public class ItemDetailsActivity extends Activity {
    private LostLinkDatabase database;
    private LostItem item;
    private AuthStore authStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);
        configureSystemBars();
        database = new LostLinkDatabase(this);
        authStore = new AuthStore(this);
        findViewById(R.id.detailsBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.openMapButton).setOnClickListener(view -> {
            if (item != null) openMap(item.location);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) loadItem();
    }

    private void loadItem() {
        item = database.getItem(getIntent().getLongExtra("item_id", -1L));
        if (item == null || (!"Approved".equals(item.reviewStatus)
                && !item.reporterEmail.equals(authStore.getEmail()) && !authStore.isAdmin())) {
            Toast.makeText(this, R.string.error_item_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ((TextView) findViewById(R.id.detailsType)).setText(
                "LOST".equals(item.type) ? R.string.lost_badge : R.string.found_badge);
        ((TextView) findViewById(R.id.detailsName)).setText(item.name);
        ((TextView) findViewById(R.id.detailsCategory)).setText(item.category);
        ((TextView) findViewById(R.id.detailsDescription)).setText(item.description);
        ((TextView) findViewById(R.id.detailsDate)).setText(
                getString(R.string.details_date, item.reportDate));
        ((TextView) findViewById(R.id.detailsLocation)).setText(
                getString(R.string.details_full_location, item.location,
                        valueOrEmpty(item.area), valueOrEmpty(item.district), valueOrEmpty(item.taluka)));
        ((TextView) findViewById(R.id.detailsReporter)).setText(
                getString(R.string.details_reporter, item.reporterName));
        TextView phoneText = findViewById(R.id.detailsContactPhone);
        phoneText.setVisibility(item.contactPhone == null || item.contactPhone.isEmpty()
                ? View.GONE : View.VISIBLE);
        if (item.contactPhone != null && !item.contactPhone.isEmpty()) {
            phoneText.setText(getString(R.string.reporter_phone_format, item.contactPhone));
        }

        ((TextView) findViewById(R.id.detailsStatus)).setText(
                getString(R.string.details_status, item.status));
        TextView reviewStatus = findViewById(R.id.detailsReviewStatus);
        reviewStatus.setText(getString(R.string.review_status_format, item.reviewStatus));
        reviewStatus.setVisibility("Approved".equals(item.reviewStatus) ? View.GONE : View.VISIBLE);

        ImageView image = findViewById(R.id.detailsPhoto);
        image.setImageResource(R.drawable.ic_lostlink);
        if (item.photoUri != null) {
            try {
                image.setImageURI(Uri.parse(item.photoUri));
            } catch (SecurityException | IllegalArgumentException exception) {
                image.setImageResource(R.drawable.ic_lostlink);
            }
        }
        View videoButton = findViewById(R.id.playVideoButton);
        videoButton.setVisibility(item.videoUri == null ? View.GONE : View.VISIBLE);
        videoButton.setOnClickListener(view -> playVideo(item.videoUri));
        configureItemActions();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void playVideo(String videoUri) {
        if (videoUri == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(videoUri), "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(this, R.string.error_video_player_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void configureItemActions() {
        boolean isOwner = item.reporterEmail.equals(authStore.getEmail());
        View editButton = findViewById(R.id.editReportButton);
        View deleteButton = findViewById(R.id.deleteReportButton);
        editButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        editButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, ReportItemActivity.class);
            intent.putExtra("item_type", item.type);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        });
        deleteButton.setOnClickListener(view -> confirmDelete());

        Button claimButton = findViewById(R.id.claimItemButton);
        String claimStatus = database.getActiveClaimStatus(item.id, authStore.getEmail());
        boolean canInteract = "Approved".equals(item.reviewStatus);
        claimButton.setVisibility(!isOwner && canInteract && !"Resolved".equals(item.status)
                ? View.VISIBLE : View.GONE);
        if (claimStatus != null) {
            claimButton.setText(getString(R.string.claim_already_status, claimStatus));
            claimButton.setEnabled(false);
        } else {
            claimButton.setText(R.string.request_claim);
            claimButton.setEnabled(true);
            claimButton.setOnClickListener(view -> showClaimDialog());
        }
        if (!isOwner && !canInteract) {
            findViewById(R.id.contactOwnerButton).setVisibility(View.GONE);
            findViewById(R.id.emailOwnerButton).setVisibility(View.GONE);
            findViewById(R.id.callReporterButton).setVisibility(View.GONE);
            findViewById(R.id.smsReporterButton).setVisibility(View.GONE);
            findViewById(R.id.possibleMatchesButton).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.contactOwnerButton).setVisibility(isOwner ? View.GONE : View.VISIBLE);
        findViewById(R.id.contactOwnerButton).setOnClickListener(view -> openConversation());
        findViewById(R.id.emailOwnerButton).setVisibility(isOwner ? View.GONE : View.VISIBLE);
        findViewById(R.id.emailOwnerButton).setOnClickListener(view -> emailReporter());
        boolean canCallOrText = !isOwner && item.contactPhone != null && !item.contactPhone.isEmpty();
        findViewById(R.id.callReporterButton).setVisibility(
                canCallOrText ? View.VISIBLE : View.GONE);
        findViewById(R.id.callReporterButton).setOnClickListener(view -> callReporter());
        findViewById(R.id.smsReporterButton).setVisibility(
                canCallOrText ? View.VISIBLE : View.GONE);
        findViewById(R.id.smsReporterButton).setOnClickListener(view -> textReporter());
        findViewById(R.id.manageClaimsButton).setVisibility(isOwner ? View.VISIBLE : View.GONE);
        findViewById(R.id.manageClaimsButton).setOnClickListener(view ->
                startActivity(new Intent(this, ClaimsActivity.class)));
        findViewById(R.id.possibleMatchesButton).setVisibility(
                canInteract ? View.VISIBLE : View.GONE);
        findViewById(R.id.possibleMatchesButton).setOnClickListener(view -> {
            Intent intent = new Intent(this, PotentialMatchesActivity.class);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        });
    }

    private void showClaimDialog() {
        EditText messageInput = new EditText(this);
        messageInput.setHint(R.string.claim_message_hint);
        messageInput.setMinLines(3);
        messageInput.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        messageInput.setPadding(padding, padding, padding, padding);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.request_claim)
                .setMessage(getString(R.string.claim_for_item, item.name))
                .setView(messageInput)
                .setNegativeButton(R.string.cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.send_claim, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> submitClaim(messageInput, dialog)));
        dialog.show();
    }

    private void submitClaim(EditText messageInput, AlertDialog dialog) {
        String message = messageInput.getText().toString().trim();
        if (message.length() < 8) {
            messageInput.setError(getString(R.string.error_claim_message));
            return;
        }
        try {
            long claimId = database.createClaim(item.id, authStore.getEmail(),
                    authStore.getDisplayName(), message);
            if (claimId < 0) {
                Toast.makeText(this, R.string.error_claim_exists, Toast.LENGTH_LONG).show();
                return;
            }
            String notificationMessage = getString(R.string.new_claim_notification, item.name);
            LocalNotifier.showForSignedInUser(this, item.reporterEmail,
                    getString(R.string.new_claim_title), notificationMessage, item.id);
            Toast.makeText(this, R.string.claim_submitted, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadItem();
        } catch (android.database.sqlite.SQLiteException exception) {
            Toast.makeText(this, R.string.error_claim_submit, Toast.LENGTH_LONG).show();
        }
    }

    private void openConversation() {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("item_id", item.id);
        intent.putExtra("recipient_email", item.reporterEmail);
        intent.putExtra("recipient_name", item.reporterName);
        startActivity(intent);
    }

    private void emailReporter() {
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", item.reporterEmail, null));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject, item.name));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.error_email_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void callReporter() {
        Intent intent = new Intent(Intent.ACTION_DIAL,
                Uri.fromParts("tel", item.contactPhone, null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.error_phone_app_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void textReporter() {
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("smsto", item.contactPhone, null));
        intent.putExtra("sms_body", getString(R.string.sms_item_intro, item.name));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.error_phone_app_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_report_title)
                .setMessage(R.string.delete_report_confirmation)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.delete_report, (dialog, which) -> {
                    if (database.deleteItem(item.id, authStore.getEmail())) {
                        Toast.makeText(this, R.string.report_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.error_item_unavailable, Toast.LENGTH_LONG).show();
                        loadItem();
                    }
                })
                .show();
    }

    private void openMap(String location) {
        Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
        Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.error_map_unavailable, Toast.LENGTH_SHORT).show();
        }
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
