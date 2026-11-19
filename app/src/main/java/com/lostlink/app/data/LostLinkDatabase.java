package com.lostlink.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public final class LostLinkDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "lostlink.db";
    private static final int DATABASE_VERSION = 6;

    public LostLinkDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Users (email TEXT PRIMARY KEY, name TEXT NOT NULL, "
                + "created_at TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'student', "
                + "mobile TEXT, address TEXT, username TEXT)");
        db.execSQL("CREATE TABLE Items (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "type TEXT NOT NULL CHECK(type IN ('LOST','FOUND')), name TEXT NOT NULL, "
                + "category TEXT NOT NULL, description TEXT NOT NULL, report_date TEXT NOT NULL, "
                + "location TEXT NOT NULL, area TEXT, district TEXT, taluka TEXT, "
                + "ownership_proof TEXT, finding_evidence TEXT, "
                + "contact_phone TEXT, photo_uri TEXT, video_uri TEXT, "
                + "review_status TEXT NOT NULL DEFAULT 'Approved', reporter_email TEXT NOT NULL, "
                + "reporter_name TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'Pending', "
                + "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_items_type_date ON Items(type, created_at DESC)");
        db.execSQL("CREATE INDEX idx_items_reporter ON Items(reporter_email, created_at DESC)");
        db.execSQL("CREATE INDEX idx_items_status ON Items(status)");
        db.execSQL("CREATE INDEX idx_items_review_status ON Items(review_status, created_at DESC)");
        db.execSQL("CREATE TABLE Claims (id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER NOT NULL, "
                + "claimant_email TEXT NOT NULL, message TEXT NOT NULL, admin_note TEXT, "
                + "status TEXT NOT NULL DEFAULT 'Pending', "
                + "created_at INTEGER NOT NULL, FOREIGN KEY(item_id) REFERENCES Items(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_claims_claimant ON Claims(claimant_email, created_at DESC)");
        db.execSQL("CREATE INDEX idx_claims_item ON Claims(item_id, status)");
        db.execSQL("CREATE TABLE Notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT NOT NULL, title TEXT NOT NULL, message TEXT NOT NULL, "
                + "is_read INTEGER NOT NULL DEFAULT 0, related_item_id INTEGER, created_at INTEGER NOT NULL, "
                + "FOREIGN KEY(related_item_id) REFERENCES Items(id) ON DELETE SET NULL)");
        db.execSQL("CREATE INDEX idx_notifications_user ON Notifications(user_email, created_at DESC)");
        db.execSQL("CREATE TABLE Messages (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sender_email TEXT NOT NULL, recipient_email TEXT NOT NULL, item_id INTEGER, "
                + "message TEXT NOT NULL, created_at INTEGER NOT NULL, "
                + "FOREIGN KEY(item_id) REFERENCES Items(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE Feedback (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL, "
                + "message TEXT NOT NULL, admin_reply TEXT, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_feedback_user ON Feedback(user_email, created_at DESC)");
        insertSampleItems(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_status ON Items(status)");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE Notifications ADD COLUMN related_item_id INTEGER");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_claims_claimant ON Claims(claimant_email, created_at DESC)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_claims_item ON Claims(item_id, status)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_user ON Notifications(user_email, created_at DESC)");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE Items ADD COLUMN contact_phone TEXT");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE Items ADD COLUMN video_uri TEXT");
            db.execSQL("ALTER TABLE Items ADD COLUMN review_status TEXT NOT NULL DEFAULT 'Approved'");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_review_status "
                    + "ON Items(review_status, created_at DESC)");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE Users ADD COLUMN mobile TEXT");
            db.execSQL("ALTER TABLE Users ADD COLUMN address TEXT");
            db.execSQL("ALTER TABLE Users ADD COLUMN username TEXT");
            db.execSQL("ALTER TABLE Items ADD COLUMN area TEXT");
            db.execSQL("ALTER TABLE Items ADD COLUMN district TEXT");
            db.execSQL("ALTER TABLE Items ADD COLUMN taluka TEXT");
            db.execSQL("ALTER TABLE Items ADD COLUMN ownership_proof TEXT");
            db.execSQL("ALTER TABLE Items ADD COLUMN finding_evidence TEXT");
            db.execSQL("ALTER TABLE Claims ADD COLUMN admin_note TEXT");
            db.execSQL("CREATE TABLE Feedback (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_email TEXT NOT NULL, name TEXT NOT NULL, email TEXT NOT NULL, "
                    + "message TEXT NOT NULL, admin_reply TEXT, created_at INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX idx_feedback_user ON Feedback(user_email, created_at DESC)");
        }
    }

    public void saveUser(String email, String name) {
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("name", name);
        values.put("role", "admin@lostlink.edu".equalsIgnoreCase(email) ? "admin" : "student");
        values.put("created_at", dateToday());
        getWritableDatabase().insertWithOnConflict("Users", null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        ContentValues updated = new ContentValues();
        updated.put("name", name);
        updated.put("role", "admin@lostlink.edu".equalsIgnoreCase(email) ? "admin" : "student");
        getWritableDatabase().update("Users", updated, "email = ?", new String[]{email});
    }

    public void saveUserProfile(String email, String name, String mobile, String address,
                                String username) {
        saveUser(email, name);
        ContentValues values = new ContentValues();
        values.put("mobile", mobile);
        values.put("address", address);
        values.put("username", username);
        getWritableDatabase().update("Users", values, "email = ?", new String[]{email});
    }

    public long createItem(String type, String name, String category, String description,
                           String reportDate, String location, String photoUri,
                           String videoUri, String reporterEmail, String reporterName,
                           String contactPhone, String area, String district, String taluka,
                           String ownershipProof, String findingEvidence) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("name", name);
        values.put("category", category);
        values.put("description", description);
        values.put("report_date", reportDate);
        values.put("location", location);
        values.put("area", area);
        values.put("district", district);
        values.put("taluka", taluka);
        values.put("ownership_proof", ownershipProof);
        values.put("finding_evidence", findingEvidence);
        values.put("contact_phone", contactPhone);
        values.put("photo_uri", photoUri);
        values.put("video_uri", videoUri);
        values.put("review_status", "Pending");
        values.put("reporter_email", reporterEmail);
        values.put("reporter_name", reporterName);
        values.put("status", "Pending");
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("Items", null, values);
    }

    public boolean updateItem(long id, String reporterEmail, String name, String category,
                              String description, String reportDate, String location, String photoUri,
                              String videoUri, String contactPhone, String area, String district,
                              String taluka, String ownershipProof, String findingEvidence) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("category", category);
        values.put("description", description);
        values.put("report_date", reportDate);
        values.put("location", location);
        values.put("area", area);
        values.put("district", district);
        values.put("taluka", taluka);
        values.put("ownership_proof", ownershipProof);
        values.put("finding_evidence", findingEvidence);
        values.put("contact_phone", contactPhone);
        values.put("photo_uri", photoUri);
        values.put("video_uri", videoUri);
        values.put("review_status", "Pending");
        return getWritableDatabase().update("Items", values,
                "id = ? AND reporter_email = ?",
                new String[]{String.valueOf(id), reporterEmail}) == 1;
    }

    public ArrayList<String[]> findUsers() {
        ArrayList<String[]> users = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("Users",
                new String[]{"email", "name", "mobile", "address", "username", "created_at"},
                "role = 'student'", null, null, null, "created_at DESC, name COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                users.add(new String[]{cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3), cursor.getString(4),
                        cursor.getString(5)});
            }
        }
        return users;
    }

    public int countUsers() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM Users WHERE role = 'student'", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public boolean deleteItem(long id, String reporterEmail) {
        return getWritableDatabase().delete("Items", "id = ? AND reporter_email = ?",
                new String[]{String.valueOf(id), reporterEmail}) == 1;
    }

    public ArrayList<LostItem> findItems(String type, String query, String category,
                                         String reporterEmail) {
        return findItems(type, query, category, reporterEmail, false);
    }

    public ArrayList<LostItem> findItems(String type, String query, String category,
                                         String reporterEmail, boolean includeUnreviewed) {
        return findItems(type, query, null, category, reporterEmail, includeUnreviewed);
    }

    public ArrayList<LostItem> findItems(String type, String query, String locationQuery,
                                         String category, String reporterEmail,
                                         boolean includeUnreviewed) {
        ArrayList<LostItem> items = new ArrayList<>();
        StringBuilder selection = new StringBuilder("1=1");
        ArrayList<String> arguments = new ArrayList<>();
        if (type != null) {
            selection.append(" AND type = ?");
            arguments.add(type);
        }
        if (query != null && !query.trim().isEmpty()) {
            selection.append(" AND (name LIKE ? OR description LIKE ? OR location LIKE ? "
                    + "OR area LIKE ? OR district LIKE ? OR taluka LIKE ?)");
            String pattern = "%" + query.trim() + "%";
            arguments.add(pattern);
            arguments.add(pattern);
            arguments.add(pattern);
            arguments.add(pattern);
            arguments.add(pattern);
            arguments.add(pattern);
        }
        if (locationQuery != null && !locationQuery.trim().isEmpty()) {
            selection.append(" AND (location LIKE ? OR area LIKE ? OR district LIKE ? OR taluka LIKE ?)");
            String pattern = "%" + locationQuery.trim() + "%";
            arguments.add(pattern);
            arguments.add(pattern);
            arguments.add(pattern);
            arguments.add(pattern);
        }
        if (category != null && !category.equals("All categories")) {
            selection.append(" AND category = ?");
            arguments.add(category);
        }
        if (reporterEmail != null) {
            selection.append(" AND reporter_email = ?");
            arguments.add(reporterEmail);
        }
        if (!includeUnreviewed && reporterEmail == null) {
            selection.append(" AND review_status = 'Approved'");
        }

        try (Cursor cursor = getReadableDatabase().query("Items", null,
                selection.toString(), arguments.toArray(new String[0]), null, null,
                "created_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                items.add(readItem(cursor));
            }
        }
        return items;
    }

    public LostItem getItem(long id) {
        try (Cursor cursor = getReadableDatabase().query("Items", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? readItem(cursor) : null;
        }
    }

    public ArrayList<LostItem> findItemsForReview() {
        ArrayList<LostItem> items = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("Items", null,
                "review_status = 'Pending'", null, null, null, "created_at ASC, id ASC")) {
            while (cursor.moveToNext()) items.add(readItem(cursor));
        }
        return items;
    }

    public boolean updateReviewStatus(long itemId, String status) {
        if (!"Approved".equals(status) && !"Rejected".equals(status)) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("review_status", status);
            int updated = db.update("Items", values,
                    "id = ? AND review_status = 'Pending'",
                    new String[]{String.valueOf(itemId)});
            if (updated != 1) return false;
            LostItem item = getItem(itemId);
            if (item != null) {
                String title = "Approved".equals(status)
                        ? "Report approved" : "Report needs changes";
                String message = "Your report \"" + item.name + "\" was " + status.toLowerCase(Locale.ROOT)
                        + " by a campus administrator.";
                addNotification(item.reporterEmail, title, message, item.id);
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public int countItems(String reporterEmail, String status) {
        StringBuilder selection = new StringBuilder("1=1");
        ArrayList<String> args = new ArrayList<>();
        if (reporterEmail != null) {
            selection.append(" AND reporter_email = ?");
            args.add(reporterEmail);
        }
        if (status != null) {
            selection.append(" AND status = ?");
            args.add(status);
        }
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM Items WHERE " + selection, args.toArray(new String[0]))) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public long createClaim(long itemId, String claimantEmail, String claimantName, String message) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            LostItem item = getItem(itemId);
            if (item == null || !"Approved".equals(item.reviewStatus)
                    || item.reporterEmail.equals(claimantEmail)
                    || "Resolved".equals(item.status) || hasActiveClaim(itemId, claimantEmail)) {
                return -1L;
            }
            saveUser(claimantEmail, claimantName);
            ContentValues values = new ContentValues();
            values.put("item_id", itemId);
            values.put("claimant_email", claimantEmail);
            values.put("message", message);
            values.put("status", "Pending");
            values.put("created_at", System.currentTimeMillis());
            long claimId = db.insertOrThrow("Claims", null, values);
            addNotification(item.reporterEmail, "New claim request",
                    claimantName + " is interested in \"" + item.name + "\".", itemId);
            db.setTransactionSuccessful();
            return claimId;
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<Claim> findClaims(String userEmail, boolean incoming) {
        ArrayList<Claim> claims = new ArrayList<>();
        String emailColumn = incoming ? "i.reporter_email" : "c.claimant_email";
        String sql = "SELECT c.id, c.item_id, i.name, i.type, c.claimant_email, "
                + "COALESCE(u.name, c.claimant_email), i.reporter_email, c.message, "
                + "c.admin_note, c.status, c.created_at "
                + "FROM Claims c JOIN Items i ON i.id = c.item_id "
                + "LEFT JOIN Users u ON u.email = c.claimant_email WHERE " + emailColumn
                + " = ? ORDER BY c.created_at DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{userEmail})) {
            while (cursor.moveToNext()) {
                claims.add(new Claim(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getString(7), cursor.getString(8),
                        cursor.getString(9), cursor.getLong(10)));
            }
        }
        return claims;
    }

    public boolean updateClaimStatus(long claimId, String actorEmail, String newStatus) {
        if (!"Approved".equals(newStatus) && !"Rejected".equals(newStatus)
                && !"Resolved".equals(newStatus)) {
            return false;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Claim claim = getClaim(claimId);
            if (claim == null) {
                return false;
            }
            boolean isOwner = claim.ownerEmail.equals(actorEmail);
            boolean isClaimant = claim.claimantEmail.equals(actorEmail);
            if (("Approved".equals(newStatus) || "Rejected".equals(newStatus))
                    && (!isOwner || !"Pending".equals(claim.status))) {
                return false;
            }
            if ("Approved".equals(newStatus) && hasOtherApprovedClaim(claim.itemId, claimId)) {
                return false;
            }
            if ("Resolved".equals(newStatus)
                    && (!isOwner && !isClaimant || !"Approved".equals(claim.status))) {
                return false;
            }

            ContentValues values = new ContentValues();
            values.put("status", newStatus);
            int updated = db.update("Claims", values, "id = ? AND status = ?",
                    new String[]{String.valueOf(claimId), claim.status});
            if (updated != 1) {
                return false;
            }
            if ("Approved".equals(newStatus)) {
                rejectOtherPendingClaims(db, claim.itemId, claimId, claim.itemName);
            }
            String targetEmail = isOwner ? claim.claimantEmail : claim.ownerEmail;
            String title;
            if ("Approved".equals(newStatus)) {
                title = "Claim approved";
            } else if ("Rejected".equals(newStatus)) {
                title = "Claim update";
            } else {
                title = "Item marked resolved";
                ContentValues itemValues = new ContentValues();
                itemValues.put("status", "Resolved");
                db.update("Items", itemValues, "id = ?", new String[]{String.valueOf(claim.itemId)});
            }
            addNotification(targetEmail, title, "Update for \"" + claim.itemName
                    + "\": " + newStatus + ".", claim.itemId);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<Claim> findAllClaims() {
        ArrayList<Claim> claims = new ArrayList<>();
        String sql = "SELECT c.id, c.item_id, i.name, i.type, c.claimant_email, "
                + "COALESCE(u.name, c.claimant_email), i.reporter_email, c.message, "
                + "c.admin_note, c.status, c.created_at "
                + "FROM Claims c JOIN Items i ON i.id = c.item_id "
                + "LEFT JOIN Users u ON u.email = c.claimant_email "
                + "ORDER BY c.created_at DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                claims.add(new Claim(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getString(7), cursor.getString(8),
                        cursor.getString(9), cursor.getLong(10)));
            }
        }
        return claims;
    }

    public boolean updateClaimAsAdmin(long claimId, String newStatus, String note) {
        if (!"Approved".equals(newStatus) && !"Rejected".equals(newStatus)) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Claim claim = getClaim(claimId);
            if (claim == null || !"Pending".equals(claim.status)
                    || ("Approved".equals(newStatus) && hasOtherApprovedClaim(claim.itemId, claimId))) {
                return false;
            }
            ContentValues values = new ContentValues();
            values.put("status", newStatus);
            values.put("admin_note", note);
            int updated = db.update("Claims", values, "id = ? AND status = 'Pending'",
                    new String[]{String.valueOf(claimId)});
            if (updated != 1) return false;
            if ("Approved".equals(newStatus)) {
                rejectOtherPendingClaims(db, claim.itemId, claimId, claim.itemName);
            }
            String notification = "Admin reviewed your claim for \"" + claim.itemName + "\": "
                    + newStatus + (note == null || note.trim().isEmpty() ? "" : ". Note: " + note.trim());
            addNotification(claim.claimantEmail, "Claim reviewed", notification, claim.itemId);
            addNotification(claim.ownerEmail, "Claim reviewed", notification, claim.itemId);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public long getActiveClaimId(long itemId, String claimantEmail) {
        try (Cursor cursor = getReadableDatabase().query("Claims", new String[]{"id"},
                "item_id = ? AND claimant_email = ? AND status IN ('Pending','Approved')",
                new String[]{String.valueOf(itemId), claimantEmail}, null, null, "created_at DESC",
                "1")) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    public String getActiveClaimStatus(long itemId, String claimantEmail) {
        try (Cursor cursor = getReadableDatabase().query("Claims", new String[]{"status"},
                "item_id = ? AND claimant_email = ? AND status IN ('Pending','Approved')",
                new String[]{String.valueOf(itemId), claimantEmail}, null, null, "created_at DESC",
                "1")) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    public int countClaims(String userEmail, boolean incoming, String status) {
        String column = incoming ? "i.reporter_email" : "c.claimant_email";
        String sql = "SELECT COUNT(*) FROM Claims c JOIN Items i ON i.id = c.item_id WHERE "
                + column + " = ?" + (status == null ? "" : " AND c.status = ?");
        String[] args = status == null ? new String[]{userEmail} : new String[]{userEmail, status};
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public long addNotification(String userEmail, String title, String message, long itemId) {
        ContentValues values = new ContentValues();
        values.put("user_email", userEmail);
        values.put("title", title);
        values.put("message", message);
        values.put("is_read", 0);
        if (itemId < 0) values.putNull("related_item_id");
        else values.put("related_item_id", itemId);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("Notifications", null, values);
    }

    public ArrayList<AppNotification> findNotifications(String userEmail) {
        ArrayList<AppNotification> notifications = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("Notifications",
                new String[]{"id", "title", "message", "related_item_id", "is_read"},
                "user_email = ?", new String[]{userEmail}, null, null, "created_at DESC")) {
            while (cursor.moveToNext()) {
                notifications.add(new AppNotification(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.isNull(3) ? -1L : cursor.getLong(3),
                        cursor.getInt(4) != 0));
            }
        }
        return notifications;
    }

    public int countUnreadNotifications(String userEmail) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM Notifications WHERE user_email = ? AND is_read = 0",
                new String[]{userEmail})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void markNotificationsRead(String userEmail) {
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        getWritableDatabase().update("Notifications", values,
                "user_email = ? AND is_read = 0", new String[]{userEmail});
    }

    public boolean sendMessage(long itemId, String senderEmail, String recipientEmail, String message) {
        LostItem item = getItem(itemId);
        if (item == null || message == null || message.trim().isEmpty()
                || senderEmail.equals(recipientEmail)) {
            return false;
        }
        boolean senderIsOwner = senderEmail.equals(item.reporterEmail);
        boolean recipientIsOwner = recipientEmail.equals(item.reporterEmail);
        if (!senderIsOwner && !recipientIsOwner) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put("sender_email", senderEmail);
        values.put("recipient_email", recipientEmail);
        values.put("message", message.trim());
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("Messages", null, values);
        String senderName = senderIsOwner ? item.reporterName : getUserName(senderEmail);
        addNotification(recipientEmail, "New message",
                senderName + " sent you a message about \"" + item.name + "\".", itemId);
        return true;
    }

    public ArrayList<ChatMessage> findMessages(long itemId, String firstEmail, String secondEmail) {
        ArrayList<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT id, sender_email, message, created_at FROM Messages "
                + "WHERE item_id = ? AND ((sender_email = ? AND recipient_email = ?) "
                + "OR (sender_email = ? AND recipient_email = ?)) ORDER BY created_at ASC, id ASC";
        String[] args = {String.valueOf(itemId), firstEmail, secondEmail, secondEmail, firstEmail};
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                messages.add(new ChatMessage(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getLong(3)));
            }
        }
        return messages;
    }

    public boolean sendSystemMessage(String senderEmail, String recipientEmail, String message) {
        if (senderEmail == null || recipientEmail == null || message == null
                || message.trim().isEmpty() || senderEmail.equals(recipientEmail)
                || (!"admin@lostlink.edu".equalsIgnoreCase(senderEmail)
                && !"admin@lostlink.edu".equalsIgnoreCase(recipientEmail))) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("sender_email", senderEmail);
        values.put("recipient_email", recipientEmail);
        values.putNull("item_id");
        values.put("message", message.trim());
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("Messages", null, values);
        addNotification(recipientEmail, "Message from LostLink",
                "You have a new account message.", -1L);
        return true;
    }

    public ArrayList<ChatMessage> findSystemMessages(String firstEmail, String secondEmail) {
        ArrayList<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT id, sender_email, message, created_at FROM Messages "
                + "WHERE item_id IS NULL AND ((sender_email = ? AND recipient_email = ?) "
                + "OR (sender_email = ? AND recipient_email = ?)) ORDER BY created_at ASC, id ASC";
        String[] args = {firstEmail, secondEmail, secondEmail, firstEmail};
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                messages.add(new ChatMessage(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getLong(3)));
            }
        }
        return messages;
    }

    private boolean hasActiveClaim(long itemId, String claimantEmail) {
        try (Cursor cursor = getReadableDatabase().query("Claims", new String[]{"id"},
                "item_id = ? AND claimant_email = ? AND status IN ('Pending','Approved')",
                new String[]{String.valueOf(itemId), claimantEmail}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private boolean hasOtherApprovedClaim(long itemId, long excludedClaimId) {
        try (Cursor cursor = getReadableDatabase().query("Claims", new String[]{"id"},
                "item_id = ? AND id != ? AND status = 'Approved'",
                new String[]{String.valueOf(itemId), String.valueOf(excludedClaimId)},
                null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    private void rejectOtherPendingClaims(SQLiteDatabase db, long itemId, long approvedClaimId,
                                          String itemName) {
        ArrayList<String> claimantEmails = new ArrayList<>();
        try (Cursor cursor = db.query("Claims", new String[]{"claimant_email"},
                "item_id = ? AND id != ? AND status = 'Pending'",
                new String[]{String.valueOf(itemId), String.valueOf(approvedClaimId)},
                null, null, null)) {
            while (cursor.moveToNext()) claimantEmails.add(cursor.getString(0));
        }
        ContentValues values = new ContentValues();
        values.put("status", "Rejected");
        db.update("Claims", values, "item_id = ? AND id != ? AND status = 'Pending'",
                new String[]{String.valueOf(itemId), String.valueOf(approvedClaimId)});
        for (String claimantEmail : claimantEmails) {
            addNotification(claimantEmail, "Claim update",
                    "Another request was approved for \"" + itemName + "\".", itemId);
        }
    }

    private Claim getClaim(long claimId) {
        String sql = "SELECT c.id, c.item_id, i.name, i.type, c.claimant_email, "
                + "COALESCE(u.name, c.claimant_email), i.reporter_email, c.message, "
                + "c.admin_note, c.status, c.created_at "
                + "FROM Claims c JOIN Items i ON i.id = c.item_id "
                + "LEFT JOIN Users u ON u.email = c.claimant_email WHERE c.id = ?";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(claimId)})) {
            if (!cursor.moveToFirst()) return null;
            return new Claim(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7), cursor.getString(8),
                    cursor.getString(9), cursor.getLong(10));
        }
    }

    public long submitFeedback(String userEmail, String name, String email, String message) {
        if (userEmail == null || name == null || name.trim().isEmpty()
                || email == null || message == null || message.trim().isEmpty()
                || message.trim().length() > 2000) {
            return -1L;
        }
        ContentValues values = new ContentValues();
        values.put("user_email", userEmail);
        values.put("name", name.trim());
        values.put("email", email.trim());
        values.put("message", message.trim());
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("Feedback", null, values);
    }

    public ArrayList<FeedbackEntry> findFeedback(String userEmail) {
        ArrayList<FeedbackEntry> entries = new ArrayList<>();
        String selection = userEmail == null ? null : "user_email = ?";
        String[] args = userEmail == null ? null : new String[]{userEmail};
        try (Cursor cursor = getReadableDatabase().query("Feedback", null, selection, args,
                null, null, "created_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                entries.add(new FeedbackEntry(cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("user_email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("message")),
                        cursor.getString(cursor.getColumnIndexOrThrow("admin_reply")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))));
            }
        }
        return entries;
    }

    public boolean replyToFeedback(long feedbackId, String reply) {
        if (reply == null || reply.trim().isEmpty() || reply.trim().length() > 2000) return false;
        ContentValues values = new ContentValues();
        values.put("admin_reply", reply.trim());
        int updated = getWritableDatabase().update("Feedback", values, "id = ?",
                new String[]{String.valueOf(feedbackId)});
        if (updated != 1) return false;
        try (Cursor cursor = getReadableDatabase().query("Feedback",
                new String[]{"user_email"}, "id = ?",
                new String[]{String.valueOf(feedbackId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                String email = cursor.getString(0);
                addNotification(email, "Feedback reply",
                        "An administrator replied to your feedback.", -1L);
            }
        }
        return true;
    }

    public boolean deleteUserData(String email) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ArrayList<Long> ownedItems = new ArrayList<>();
            try (Cursor cursor = db.query("Items", new String[]{"id"}, "reporter_email = ?",
                    new String[]{email}, null, null, null)) {
                while (cursor.moveToNext()) ownedItems.add(cursor.getLong(0));
            }
            for (Long itemId : ownedItems) {
                db.delete("Items", "id = ?", new String[]{String.valueOf(itemId)});
            }
            db.delete("Claims", "claimant_email = ?", new String[]{email});
            db.delete("Notifications", "user_email = ?", new String[]{email});
            db.delete("Messages", "sender_email = ? OR recipient_email = ?", new String[]{email, email});
            db.delete("Feedback", "user_email = ?", new String[]{email});
            db.delete("Users", "email = ?", new String[]{email});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteItemAsAdmin(long itemId) {
        return getWritableDatabase().delete("Items", "id = ?",
                new String[]{String.valueOf(itemId)}) == 1;
    }

    public boolean updateItemStatusAsAdmin(long itemId, String status) {
        if (!"Pending".equals(status) && !"Resolved".equals(status)) return false;
        ContentValues values = new ContentValues();
        values.put("status", status);
        return getWritableDatabase().update("Items", values, "id = ?",
                new String[]{String.valueOf(itemId)}) == 1;
    }

    private String getUserName(String email) {
        try (Cursor cursor = getReadableDatabase().query("Users", new String[]{"name"},
                "email = ?", new String[]{email}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : email;
        }
    }

    private static LostItem readItem(Cursor cursor) {
        return new LostItem(cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("type")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("category")),
                cursor.getString(cursor.getColumnIndexOrThrow("description")),
                cursor.getString(cursor.getColumnIndexOrThrow("report_date")),
                cursor.getString(cursor.getColumnIndexOrThrow("location")),
                cursor.getString(cursor.getColumnIndexOrThrow("area")),
                cursor.getString(cursor.getColumnIndexOrThrow("district")),
                cursor.getString(cursor.getColumnIndexOrThrow("taluka")),
                cursor.getString(cursor.getColumnIndexOrThrow("ownership_proof")),
                cursor.getString(cursor.getColumnIndexOrThrow("finding_evidence")),
                cursor.getString(cursor.getColumnIndexOrThrow("contact_phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("photo_uri")),
                cursor.getString(cursor.getColumnIndexOrThrow("video_uri")),
                cursor.getString(cursor.getColumnIndexOrThrow("reporter_email")),
                cursor.getString(cursor.getColumnIndexOrThrow("reporter_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getString(cursor.getColumnIndexOrThrow("review_status")));
    }

    private static void insertSampleItems(SQLiteDatabase db) {
        String[][] samples = {
                {"FOUND", "Blue water bottle", "Personal items", "A blue insulated bottle left near the library entrance.",
                        "2026-10-04", "Central Library", "Campus Helper", "helper@lostlink.edu"},
                {"LOST", "Wireless earbuds case", "Electronics", "Small white charging case with a tiny sticker on the lid.",
                        "2026-10-03", "Student Centre", "Demo Student", "student@lostlink.edu"},
                {"FOUND", "Calculus notebook", "Books & stationery", "Green notebook with lecture notes inside.",
                        "2026-10-02", "Engineering Building, Room 204", "Campus Helper", "helper@lostlink.edu"},
                {"LOST", "Black umbrella", "Clothing & accessories", "Compact black umbrella, possibly left after class.",
                        "2026-10-01", "West Lecture Hall", "Demo Student", "student@lostlink.edu"}
        };
        long now = System.currentTimeMillis();
        for (int i = 0; i < samples.length; i++) {
            String[] sample = samples[i];
            ContentValues values = new ContentValues();
            values.put("type", sample[0]);
            values.put("name", sample[1]);
            values.put("category", sample[2]);
            values.put("description", sample[3]);
            values.put("report_date", sample[4]);
            values.put("location", sample[5]);
            values.putNull("photo_uri");
            values.putNull("video_uri");
            values.put("review_status", "Approved");
            values.put("reporter_name", sample[6]);
            values.put("reporter_email", sample[7]);
            values.put("status", "Pending");
            values.put("created_at", now - (i * 60_000L));
            db.insertOrThrow("Items", null, values);
        }
    }

    private static String dateToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
