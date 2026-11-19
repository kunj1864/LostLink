package com.lostlink.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.Claim;
import com.lostlink.app.data.LostLinkDatabase;

import java.util.ArrayList;

public final class AdminClaimsActivity extends Activity {
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
        ((TextView) findViewById(R.id.adminListTitle)).setText(R.string.admin_claims_title);
        ((TextView) findViewById(R.id.adminListEmpty)).setText(R.string.admin_claims_empty);
        list = findViewById(R.id.adminListContent);
        emptyMessage = findViewById(R.id.adminListEmpty);
        findViewById(R.id.adminListBackButton).setOnClickListener(view -> finish());
        refreshClaims();
    }

    private void refreshClaims() {
        ArrayList<Claim> claims = database.findAllClaims();
        list.removeAllViews();
        for (Claim claim : claims) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int padding = dp(15);
            card.setPadding(padding, padding, padding, padding);
            card.setBackgroundResource(R.drawable.bg_home_surface);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(10);
            card.setLayoutParams(params);
            addText(card, claim.itemName + " · " + claim.status, 17, true);
            addText(card, getString(R.string.admin_claim_people, claim.claimantName,
                    claim.ownerEmail), 13, false);
            addText(card, claim.message, 14, false);
            if (claim.adminNote != null && !claim.adminNote.trim().isEmpty()) {
                addText(card, getString(R.string.admin_claim_note, claim.adminNote), 13, false);
            }
            if ("Pending".equals(claim.status)) {
                EditText note = new EditText(this);
                note.setHint(R.string.admin_claim_note_hint);
                note.setMinLines(2);
                note.setGravity(android.view.Gravity.TOP);
                card.addView(note);
                LinearLayout actions = new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                Button approve = new Button(this);
                approve.setText(R.string.admin_approve);
                Button reject = new Button(this);
                reject.setText(R.string.admin_reject);
                actions.addView(approve, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                actions.addView(reject, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                card.addView(actions);
                approve.setOnClickListener(view -> updateClaim(claim, "Approved", note.getText().toString()));
                reject.setOnClickListener(view -> updateClaim(claim, "Rejected", note.getText().toString()));
            }
            list.addView(card);
        }
        emptyMessage.setVisibility(claims.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateClaim(Claim claim, String status, String note) {
        if (!database.updateClaimAsAdmin(claim.id, status, note)) {
            Toast.makeText(this, R.string.error_claim_update, Toast.LENGTH_LONG).show();
            refreshClaims();
            return;
        }
        Toast.makeText(this, R.string.claim_update_saved, Toast.LENGTH_SHORT).show();
        refreshClaims();
    }

    private void addText(LinearLayout card, String text, int size, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
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
