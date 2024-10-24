package com.example.clickngo.models;

public class UserProfile {

    private String displayName;
    private String bio;
    private String profileImageUrl;
    private String email;
    private String phoneNumber;
    private String joinedDate;
    private String profileImageBase64;

    // Default constructor required for Firestore data mapping
    public UserProfile() {
        // No-arg constructor for Firestore
    }

    public UserProfile(String displayName, String bio, String profileImageUrl, String email, String phoneNumber, String joinedDate, String profileImageBase64) {
        this.displayName = displayName;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.joinedDate = joinedDate;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getUsername() {
        return displayName;
    }

    public void setUsername(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(String joinedDate) {
        this.joinedDate = joinedDate;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getProfileImageBase64(){
        return profileImageBase64;
    }
}
