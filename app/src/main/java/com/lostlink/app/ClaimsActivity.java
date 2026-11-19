package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.Claim;
import com.lostlink.app.data.LocalNotifier;
import com.lostlink.app.data.LostLinkDatabase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ClaimsActivity extends Activity {
    private AuthStore authStore;
    private LostLinkDatabase database;
    private LinearLayout claimsList;
    private TextView emptyMessage;
    private TextView title;
    private boolean showingIncoming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claims);
        configureSystemBars();
        authStore = new AuthStore(this);
        database = new LostLinkDatabase(this);
        claimsList = findViewById(R.id.claimsList);
        emptyMessage = findViewById(R.id.emptyClaimsMessage);
        title = findViewById(R.id.claimsTitle);

        findViewById(R.id.claimsBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.myClaimsTab).setOnClickListener(view -> {
            showingIncoming = false;
            refreshClaims();
        });
        findViewById(R.id.incomingClaimsTab).setOnClickListener(view -> {
            showingIncoming = true;
            refreshClaims();
        });
        findViewById(R.id.myClaimsTab).setBackgroundResource(
                showingIncoming ? R.drawable.bg_card : R.drawable.bg_icon_tile_blue);
        findViewById(R.id.incomingClaimsTab).setBackgroundResource(
                showingIncoming ? R.drawable.bg_icon_tile_blue : R.drawable.bg_card);
        refreshClaims();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshClaims();
    }

    private void refreshClaims() {
        title.setText(showingIncoming ? R.string.incoming_claims_title : R.string.my_claims_title);
        findViewById(R.id.myClaimsTab).setBackgroundResource(
                showingIncoming ? R.drawable.bg_card : R.drawable.bg_icon_tile_blue);
        findViewById(R.id.incomingClaimsTab).setBackgroundResource(
                showingIncoming ? R.drawable.bg_icon_tile_blue : R.drawable.bg_card);
        ArrayList<Claim> claims = database.findClaims(authStore.getEmail(), showingIncoming);
        claimsList.removeAllViews();
        for (Claim claim : claims) {
            claimsList.addView(createClaimCard(claim));
        }
        emptyMessage.setVisibility(claims.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View createClaimCard(Claim claim) {
        View card = getLayoutInflater().inflate(R.layout.row_claim, claimsList, false);
        ((TextView) card.findViewById(R.id.claimItemName)).setText(claim.itemName);
        ((TextView) card.findViewById(R.id.claimStatus)).setText(
                getString(R.string.claim_status_format, claim.status));
        ((TextView) card.findViewById(R.id.claimMessage)).setText(claim.message);
        String person = showingIncoming
                ? getString(R.string.claimed_by, claim.claimantName)
                : getString(R.string.claimed_from, claim.ownerEmail);
        ((TextView) card.findViewById(R.id.claimPerson)).setText(person);
        ((TextView) card.findViewById(R.id.claimDate)).setText(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(claim.createdAt)));

        Button approve = card.findViewById(R.id.approveClaimButton);
        Button reject = card.findViewById(R.id.rejectClaimButton);
        Button resolve = card.findViewById(R.id.resolveClaimButton);
        Button message = card.findViewById(R.id.messageClaimButton);
        boolean canDecide = showingIncoming && "Pending".equals(claim.status);
        approve.setVisibility(canDecide ? View.VISIBLE : View.GONE);
        reject.setVisibility(canDecide ? View.VISIBLE : View.GONE);
        resolve.setVisibility("Approved".equals(claim.status) ? View.VISIBLE : View.GONE);
        approve.setOnClickListener(view -> changeStatus(claim, "Approved"));
        reject.setOnClickListener(view -> changeStatus(claim, "Rejected"));
        resolve.setOnClickListener(view -> changeStatus(claim, "Resolved"));
        message.setOnClickListener(view -> openMessages(claim));
        return card;
    }

    private void changeStatus(Claim claim, String status) {
        if (!database.updateClaimStatus(claim.id, authStore.getEmail(), status)) {
            Toast.makeText(this, R.string.error_claim_update, Toast.LENGTH_LONG).show();
            refreshClaims();
            return;
        }
        String recipient = authStore.getEmail().equals(claim.ownerEmail)
                ? claim.claimantEmail : claim.ownerEmail;
        String title = "Resolved".equals(status) ? getString(R.string.claim_resolved_title)
                : getString(R.string.claim_update_title);
        LocalNotifier.showForSignedInUser(this, recipient, title,
                getString(R.string.claim_notification, claim.itemName, status), claim.itemId);
        Toast.makeText(this, R.string.claim_update_saved, Toast.LENGTH_SHORT).show();
        refreshClaims();
    }

    private void openMessages(Claim claim) {
        String recipient = authStore.getEmail().equals(claim.ownerEmail)
                ? claim.claimantEmail : claim.ownerEmail;
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("item_id", claim.itemId);
        intent.putExtra("recipient_email", recipient);
        intent.putExtra("recipient_name", showingIncoming ? claim.claimantName : claim.ownerEmail);
        startActivity(intent);
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
