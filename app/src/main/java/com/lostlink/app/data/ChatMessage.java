package com.lostlink.app.data;

public final class ChatMessage {
    public final long id;
    public final String senderEmail;
    public final String message;
    public final long createdAt;

    public ChatMessage(long id, String senderEmail, String message, long createdAt) {
        this.id = id;
        this.senderEmail = senderEmail;
        this.message = message;
        this.createdAt = createdAt;
    }
}
