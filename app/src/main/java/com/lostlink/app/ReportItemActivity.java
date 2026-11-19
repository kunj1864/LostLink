package com.lostlink.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lostlink.app.data.AuthStore;
import com.lostlink.app.data.LostItem;
import com.lostlink.app.data.LostLinkDatabase;

import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportItemActivity extends Activity {
    private static final int REQUEST_PHOTO = 410;
    private static final int REQUEST_VIDEO = 411;
    private EditText nameInput;
    private EditText descriptionInput;
    private EditText dateInput;
    private EditText locationInput;
    private EditText areaInput;
    private EditText districtInput;
    private EditText talukaInput;
    private EditText proofInput;
    private EditText evidenceInput;
    private EditText contactPhoneInput;
    private Spinner categorySpinner;
    private ImageView photoPreview;
    private String type;
    private Uri selectedPhoto;
    private Uri selectedVideo;
    private long editingItemId = -1L;
    private AuthStore authStore;
    private LostLinkDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_item);
        configureSystemBars();

        type = getIntent().getStringExtra("item_type");
        editingItemId = getIntent().getLongExtra("item_id", -1L);
        authStore = new AuthStore(this);
        database = new LostLinkDatabase(this);
        if (!"LOST".equals(type) && !"FOUND".equals(type)) {
            database.close();
            finish();
            return;
        }

        TextView title = findViewById(R.id.reportTitle);
        title.setText("LOST".equals(type)
                ? getString(R.string.report_lost_title) : getString(R.string.report_found_title));
        nameInput = findViewById(R.id.itemNameInput);
        descriptionInput = findViewById(R.id.itemDescriptionInput);
        dateInput = findViewById(R.id.itemDateInput);
        locationInput = findViewById(R.id.itemLocationInput);
        areaInput = findViewById(R.id.itemAreaInput);
        districtInput = findViewById(R.id.itemDistrictInput);
        talukaInput = findViewById(R.id.itemTalukaInput);
        proofInput = findViewById(R.id.itemOwnershipProofInput);
        evidenceInput = findViewById(R.id.itemFindingEvidenceInput);
        contactPhoneInput = findViewById(R.id.itemContactPhoneInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        photoPreview = findViewById(R.id.photoPreview);

        String[] categories = getResources().getStringArray(R.array.report_categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        findViewById(R.id.ownershipProofLabel).setVisibility(
                "LOST".equals(type) ? View.VISIBLE : View.GONE);
        proofInput.setVisibility("LOST".equals(type) ? View.VISIBLE : View.GONE);
        findViewById(R.id.findingEvidenceLabel).setVisibility(
                "FOUND".equals(type) ? View.VISIBLE : View.GONE);
        evidenceInput.setVisibility("FOUND".equals(type) ? View.VISIBLE : View.GONE);
        dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        if (editingItemId >= 0) {
            LostItem item = database.getItem(editingItemId);
            if (item == null || !item.reporterEmail.equals(authStore.getEmail())) {
                Toast.makeText(this, R.string.error_item_unavailable, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            nameInput.setText(item.name);
            descriptionInput.setText(item.description);
            dateInput.setText(item.reportDate);
            locationInput.setText(item.location);
            areaInput.setText(item.area);
            districtInput.setText(item.district);
            talukaInput.setText(item.taluka);
            proofInput.setText(item.ownershipProof);
            evidenceInput.setText(item.findingEvidence);
            if (item.contactPhone != null) contactPhoneInput.setText(item.contactPhone);
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(item.category)) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
            if (item.photoUri != null) {
                try {
                    selectedPhoto = Uri.parse(item.photoUri);
                    photoPreview.setImageURI(selectedPhoto);
                } catch (SecurityException | IllegalArgumentException exception) {
                    Toast.makeText(this, R.string.error_photo_access, Toast.LENGTH_LONG).show();
                }
            }
            if (item.videoUri != null) {
                selectedVideo = Uri.parse(item.videoUri);
                ((TextView) findViewById(R.id.videoSelectionStatus))
                        .setText(R.string.video_attached);
            }
            title.setText(R.string.edit_report_title);
            ((Button) findViewById(R.id.saveReportButton)).setText(R.string.update_report);
        }
        dateInput.setOnClickListener(view -> chooseDate());
        findViewById(R.id.choosePhotoButton).setOnClickListener(view -> choosePhoto());
        findViewById(R.id.chooseVideoButton).setOnClickListener(view -> chooseVideo());
        findViewById(R.id.saveReportButton).setOnClickListener(view -> saveReport());
        findViewById(R.id.reportBackButton).setOnClickListener(view -> finish());
    }

    private void chooseDate() {
        Calendar today = Calendar.getInstance();
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day);
            dateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.getTime()));
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_PHOTO);
    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null
                && (requestCode == REQUEST_PHOTO || requestCode == REQUEST_VIDEO)) {
            Uri chosenPhoto = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(chosenPhoto,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (requestCode == REQUEST_PHOTO) {
                    photoPreview.setImageURI(chosenPhoto);
                    selectedPhoto = chosenPhoto;
                } else {
                    selectedVideo = chosenPhoto;
                    ((TextView) findViewById(R.id.videoSelectionStatus))
                            .setText(R.string.video_attached);
                }
            } catch (SecurityException | IllegalArgumentException exception) {
                Toast.makeText(this, R.string.error_media_access, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveReport() {
        String name = nameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String date = dateInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String area = areaInput.getText().toString().trim();
        String district = districtInput.getText().toString().trim();
        String taluka = talukaInput.getText().toString().trim();
        String ownershipProof = proofInput.getText().toString().trim();
        String findingEvidence = evidenceInput.getText().toString().trim();
        String contactPhone = contactPhoneInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameInput.setError(getString(R.string.error_item_name));
            nameInput.requestFocus();
            return;
        }
        if (description.length() < 5) {
            descriptionInput.setError(getString(R.string.error_item_description));
            descriptionInput.requestFocus();
            return;
        }
        if (date.isEmpty()) {
            dateInput.setError(getString(R.string.error_item_date));
            return;
        }
        if (location.isEmpty()) {
            locationInput.setError(getString(R.string.error_item_location));
            locationInput.requestFocus();
            return;
        }
        if (area.isEmpty()) {
            areaInput.setError(getString(R.string.error_item_area));
            areaInput.requestFocus();
            return;
        }
        if (district.isEmpty()) {
            districtInput.setError(getString(R.string.error_item_district));
            districtInput.requestFocus();
            return;
        }
        if (taluka.isEmpty()) {
            talukaInput.setError(getString(R.string.error_item_taluka));
            talukaInput.requestFocus();
            return;
        }
        EditText requiredEvidence = "LOST".equals(type) ? proofInput : evidenceInput;
        String evidence = "LOST".equals(type) ? ownershipProof : findingEvidence;
        if (evidence.isEmpty()) {
            requiredEvidence.setError("LOST".equals(type)
                    ? getString(R.string.error_ownership_proof)
                    : getString(R.string.error_finding_evidence));
            requiredEvidence.requestFocus();
            return;
        }
        if (selectedPhoto == null) {
            Toast.makeText(this, R.string.error_photo_required, Toast.LENGTH_LONG).show();
            return;
        }
        if (!isValidSelectedPhoto()) return;
        if (!contactPhone.isEmpty() && !Patterns.PHONE.matcher(contactPhone).matches()) {
            contactPhoneInput.setError(getString(R.string.error_phone_number));
            contactPhoneInput.requestFocus();
            return;
        }

        try {
            database.saveUser(authStore.getEmail(), authStore.getDisplayName());
            if (editingItemId >= 0) {
                boolean updated = database.updateItem(editingItemId, authStore.getEmail(), name,
                        String.valueOf(categorySpinner.getSelectedItem()), description, date, location,
                        selectedPhoto == null ? null : selectedPhoto.toString(),
                        selectedVideo == null ? null : selectedVideo.toString(),
                        contactPhone.isEmpty() ? null : contactPhone, area, district, taluka,
                        ownershipProof.isEmpty() ? null : ownershipProof,
                        findingEvidence.isEmpty() ? null : findingEvidence);
                if (!updated) {
                    Toast.makeText(this, R.string.error_item_unavailable, Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(this, R.string.report_updated, Toast.LENGTH_SHORT).show();
            } else {
                database.createItem(type, name, String.valueOf(categorySpinner.getSelectedItem()),
                        description, date, location,
                        selectedPhoto == null ? null : selectedPhoto.toString(),
                        selectedVideo == null ? null : selectedVideo.toString(),
                        authStore.getEmail(), authStore.getDisplayName(),
                        contactPhone.isEmpty() ? null : contactPhone, area, district, taluka,
                        ownershipProof.isEmpty() ? null : ownershipProof,
                        findingEvidence.isEmpty() ? null : findingEvidence);
                Toast.makeText(this, R.string.report_awaiting_review, Toast.LENGTH_SHORT).show();
            }

            finish();
        } catch (SQLiteException exception) {
            Toast.makeText(this, R.string.error_save_report, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidSelectedPhoto() {
        String mimeType = getContentResolver().getType(selectedPhoto);
        if (!"image/jpeg".equals(mimeType) && !"image/png".equals(mimeType)
                && !"image/webp".equals(mimeType)) {
            Toast.makeText(this, R.string.error_photo_format, Toast.LENGTH_LONG).show();
            return false;
        }
        long size = -1L;
        try (android.database.Cursor cursor = getContentResolver().query(selectedPhoto,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn);
            }
            if (size < 0) {
                try (InputStream stream = getContentResolver().openInputStream(selectedPhoto)) {
                    if (stream == null) {
                        Toast.makeText(this, R.string.error_photo_access, Toast.LENGTH_LONG).show();
                        return false;
                    }
                    byte[] buffer = new byte[8192];
                    while (size <= 2L * 1024L * 1024L) {
                        int read = stream.read(buffer);
                        if (read < 0) break;
                        size += read;
                    }
                }
            }
        } catch (SecurityException | IOException exception) {
            Toast.makeText(this, R.string.error_photo_access, Toast.LENGTH_LONG).show();
            return false;
        }
        if (size > 2L * 1024L * 1024L) {
            Toast.makeText(this, R.string.error_photo_too_large, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
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
