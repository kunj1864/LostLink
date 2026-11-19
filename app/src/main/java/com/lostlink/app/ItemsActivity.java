package com.lostlink.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;

import java.util.ArrayList;

public class ItemsActivity extends Activity {
    private LostLinkDatabase database;
    private ArrayList<LostItem> visibleItems = new ArrayList<>();
    private EditText searchInput;
    private EditText locationSearchInput;
    private Spinner typeSpinner;
    private Spinner categorySpinner;
    private TextView emptyMessage;
    private String requiredType;
    private boolean myReportsOnly;
    private String userEmail;
    private final String[] typeOptions = {"All reports", "LOST", "FOUND"};
    private String[] categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);
        configureSystemBars();

        requiredType = getIntent().getStringExtra("item_type");
        myReportsOnly = getIntent().getBooleanExtra("my_reports", false);
        userEmail = new AuthStore(this).getEmail();
        database = new LostLinkDatabase(this);
        categories = getResources().getStringArray(R.array.item_categories);

        TextView title = findViewById(R.id.itemsTitle);
        title.setText(myReportsOnly ? getString(R.string.my_reports_title)
                : getString(R.string.browse_items_title));
        searchInput = findViewById(R.id.searchInput);
        locationSearchInput = findViewById(R.id.locationSearchInput);
        typeSpinner = findViewById(R.id.typeFilterSpinner);
        categorySpinner = findViewById(R.id.filterCategorySpinner);
        emptyMessage = findViewById(R.id.emptyItemsMessage);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, typeOptions);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        searchInput.setText(getIntent().getStringExtra("search_query"));
        locationSearchInput.setText(getIntent().getStringExtra("location_query"));

        LinearLayout list = findViewById(R.id.itemsList);
        list.removeAllViews();
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                refreshItems();
            }
            @Override public void afterTextChanged(Editable value) { }
        });
        locationSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                refreshItems();
            }
            @Override public void afterTextChanged(Editable value) { }
        });
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshItems();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        typeSpinner.setOnItemSelectedListener(filterListener);
        categorySpinner.setOnItemSelectedListener(filterListener);
        findViewById(R.id.itemsBackButton).setOnClickListener(view -> finish());
        refreshItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null) refreshItems();
    }

    private void refreshItems() {
        String type = requiredType;
        if (type == null && typeSpinner.getSelectedItemPosition() > 0) {
            type = typeOptions[typeSpinner.getSelectedItemPosition()];
        }
        String category = String.valueOf(categorySpinner.getSelectedItem());
        visibleItems = database.findItems(type, searchInput.getText().toString(),
                locationSearchInput.getText().toString(), category,
                myReportsOnly ? userEmail : null, new AuthStore(this).isAdmin());

        LinearLayout list = findViewById(R.id.itemsList);
        list.removeAllViews();
        for (LostItem item : visibleItems) {
            list.addView(createItemRow(item));
        }
        emptyMessage.setVisibility(visibleItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View createItemRow(LostItem item) {
        View row = getLayoutInflater().inflate(R.layout.row_item, null, false);
        TextView name = row.findViewById(R.id.rowItemName);
        TextView meta = row.findViewById(R.id.rowItemMeta);
        TextView description = row.findViewById(R.id.rowItemDescription);
        TextView badge = row.findViewById(R.id.rowItemType);
        ImageView photo = row.findViewById(R.id.rowItemPhoto);
        name.setText(item.name);
        meta.setText(getString(R.string.item_row_meta, item.category, item.location));
        description.setText(item.description);
        badge.setText("LOST".equals(item.type) ? R.string.lost_badge : R.string.found_badge);
        TextView reviewStatus = row.findViewById(R.id.rowReviewStatus);
        if (myReportsOnly && !"Approved".equals(item.reviewStatus)) {
            reviewStatus.setText(getString(R.string.review_status_format, item.reviewStatus));
            reviewStatus.setVisibility(View.VISIBLE);
        }
        if (item.photoUri != null) {
            try {
                photo.setImageURI(android.net.Uri.parse(item.photoUri));
            } catch (SecurityException | IllegalArgumentException exception) {
                photo.setImageResource(R.drawable.ic_lostlink);
            }
        }
        row.setOnClickListener(view -> {
            Intent intent = new Intent(this, ItemDetailsActivity.class);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        });
        return row;
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
