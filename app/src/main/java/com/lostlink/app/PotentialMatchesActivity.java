package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;
import com.lostlink.app.data.MatchFinder;

import java.util.ArrayList;

public class PotentialMatchesActivity extends Activity {
    private LostLinkDatabase database;
    private LostItem source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_potential_matches);
        getWindow().setStatusBarColor(getColor(R.color.background));
        getWindow().setNavigationBarColor(getColor(R.color.background));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        database = new LostLinkDatabase(this);
        findViewById(R.id.matchesBackButton).setOnClickListener(view -> finish());
        source = database.getItem(getIntent().getLongExtra("item_id", -1L));
        if (source == null || !"Approved".equals(source.reviewStatus)) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.matchesSourceName)).setText(
                getString(R.string.matches_for_item, source.name));
        renderMatches();
    }

    private void renderMatches() {
        ArrayList<MatchFinder.MatchResult> matches = new MatchFinder()
                .findMatches(database, getContentResolver(), source);
        LinearLayout list = findViewById(R.id.potentialMatchesList);
        list.removeAllViews();
        for (MatchFinder.MatchResult match : matches) {
            LostItem item = match.item;
            View row = getLayoutInflater().inflate(R.layout.row_item, list, false);
            ((TextView) row.findViewById(R.id.rowItemName)).setText(item.name);
            ((TextView) row.findViewById(R.id.rowItemMeta)).setText(
                    getString(R.string.item_row_meta, item.category, item.location));
            ((TextView) row.findViewById(R.id.rowItemDescription)).setText(item.description);
            ((TextView) row.findViewById(R.id.rowItemType)).setText(
                    "LOST".equals(item.type) ? R.string.lost_badge : R.string.found_badge);
            TextView confidence = row.findViewById(R.id.matchConfidence);
            confidence.setText(
                    getString(R.string.match_confidence, Math.round(match.score * 100.0)));
            confidence.setVisibility(View.VISIBLE);
            if (item.photoUri != null) {
                try {
                    ((ImageView) row.findViewById(R.id.rowItemPhoto)).setImageURI(Uri.parse(item.photoUri));
                } catch (SecurityException | IllegalArgumentException exception) {
                    ((ImageView) row.findViewById(R.id.rowItemPhoto))
                            .setImageResource(R.drawable.ic_lostlink);
                }
            }
            row.setOnClickListener(view -> {
                Intent intent = new Intent(this, ItemDetailsActivity.class);
                intent.putExtra("item_id", item.id);
                startActivity(intent);
            });
            list.addView(row);
        }
        findViewById(R.id.emptyMatchesMessage).setVisibility(
                matches.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
