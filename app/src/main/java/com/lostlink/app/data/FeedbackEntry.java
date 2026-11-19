package com.lostlink.app.data;

public final class FeedbackEntry {
    public final long id;
    public final String userEmail;
    public final String name;
    public final String email;
    public final String message;
    public final String adminReply;
    public final long createdAt;

    public FeedbackEntry(long id, String userEmail, String name, String email,
                         String message, String adminReply, long createdAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.name = name;
        this.email = email;
        this.message = message;
        this.adminReply = adminReply;
        this.createdAt = createdAt;
    }
}
