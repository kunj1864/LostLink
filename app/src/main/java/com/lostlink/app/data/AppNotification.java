package com.lostlink.app.data;

public final class AppNotification {
    public final long id;
    public final String title;
    public final String message;
    public final long itemId;
    public final boolean read;

    public AppNotification(long id, String title, String message, long itemId, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.itemId = itemId;
        this.read = read;
    }
}
