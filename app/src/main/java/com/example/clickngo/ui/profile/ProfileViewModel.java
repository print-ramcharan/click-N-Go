package com.example.clickngo.ui.profile;

import static com.example.clickngo.MainActivity.sanitizeEmail;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.clickngo.models.UserProfile;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<UserProfile> userData;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static final String TAG = "ProfileViewModel"; // Add tag for logging

    public ProfileViewModel() {
        userData = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadUserData();
    }

    // Method to load user data from Firestore
    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Log user information
            String email = currentUser.getEmail();
            Log.d(TAG, "Current user UID: " + currentUser.getUid());
            Log.d(TAG, "Current user email: " + email);

            if (email != null) {
                // Sanitize the email to be used as Firestore document ID
                String sanitizedEmail = sanitizeEmail(email);
                Log.d(TAG, "Sanitized email: " + sanitizedEmail);

                // Fetch data from Firestore
                Log.d(TAG, "Fetching data from Firestore for user: " + sanitizedEmail);

                db.collection("users").document(sanitizedEmail).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Log when data is fetched successfully
                                Log.d(TAG, "User data fetched successfully from Firestore for: " + sanitizedEmail);

                                // Log the raw document data for debugging
                                Log.d(TAG, "Document Snapshot data: " + documentSnapshot.getData());

                                // Fetch specific fields to check if they exist
                                String username = documentSnapshot.getString("username");
                                String bio = documentSnapshot.getString("bio");
                                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                                Log.d(TAG, "Username: " + username);
                                Log.d(TAG, "Bio: " + bio);
                                Log.d(TAG, "Profile Image URL: " + profileImageUrl);

                                // Convert the document snapshot to a UserProfile object
                                UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                                if (userProfile != null) {
                                    // Log the UserProfile object
                                    Log.d(TAG, "UserProfile object: " + userProfile.toString());

                                    // Update LiveData with the fetched user data
                                    Log.d(TAG, "Setting LiveData with fetched user profile.");
                                    userData.setValue(userProfile);
                                } else {
                                    Log.e(TAG, "UserProfile object is null, could not convert documentSnapshot to UserProfile.");
                                }
                            } else {
                                Log.e(TAG, "No such document found in Firestore for user: " + sanitizedEmail);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching user data for: " + sanitizedEmail, e);
                        });
            } else {
                Log.e(TAG, "Email is null, cannot fetch user data");
            }
        } else {
            Log.e(TAG, "No current user is logged in.");
        }
    }

    // Return the LiveData containing user profile
    public LiveData<UserProfile> getUserData() {
        return userData;
    }
}
