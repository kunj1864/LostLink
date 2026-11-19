package com.lostlink.app.data;

public final class LostItem {
    public final long id;
    public final String type;
    public final String name;
    public final String category;
    public final String description;
    public final String reportDate;
    public final String location;
    public final String area;
    public final String district;
    public final String taluka;
    public final String ownershipProof;
    public final String findingEvidence;
    public final String contactPhone;
    public final String photoUri;
    public final String videoUri;
    public final String reporterEmail;
    public final String reporterName;
    public final String status;
    public final String reviewStatus;

    public LostItem(long id, String type, String name, String category, String description,
                    String reportDate, String location, String area, String district, String taluka,
                    String ownershipProof, String findingEvidence, String contactPhone,
                    String photoUri, String videoUri, String reporterEmail, String reporterName,
                    String status, String reviewStatus) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.category = category;
        this.description = description;
        this.reportDate = reportDate;
        this.location = location;
        this.area = area;
        this.district = district;
        this.taluka = taluka;
        this.ownershipProof = ownershipProof;
        this.findingEvidence = findingEvidence;
        this.contactPhone = contactPhone;
        this.photoUri = photoUri;
        this.videoUri = videoUri;
        this.reporterEmail = reporterEmail;
        this.reporterName = reporterName;
        this.status = status;
        this.reviewStatus = reviewStatus;
    }
}
