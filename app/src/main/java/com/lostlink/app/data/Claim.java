package com.lostlink.app.data;

public final class Claim {
    public final long id;
    public final long itemId;
    public final String itemName;
    public final String itemType;
    public final String claimantEmail;
    public final String claimantName;
    public final String ownerEmail;
    public final String message;
    public final String adminNote;
    public final String status;
    public final long createdAt;

    public Claim(long id, long itemId, String itemName, String itemType, String claimantEmail,
                 String claimantName, String ownerEmail, String message, String adminNote,
                 String status, long createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.claimantEmail = claimantEmail;
        this.claimantName = claimantName;
        this.ownerEmail = ownerEmail;
        this.message = message;
        this.adminNote = adminNote;
        this.status = status;
        this.createdAt = createdAt;
    }
}
