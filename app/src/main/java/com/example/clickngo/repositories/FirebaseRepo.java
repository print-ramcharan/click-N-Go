package com.example.clickngo.repositories;

import android.util.Log;

import com.example.clickngo.models.Link;
import com.example.clickngo.services.MyFirebaseMessagingService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirebaseRepo {

    private static final String TAG = "FirebaseRepo";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference databaseReference;

    public FirebaseRepo() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("user_links");
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        MyFirebaseMessagingService.saveTokenToFirestore(token);
                        Log.d(TAG, "FCM token: " + token);
                    } else {
                        Log.e(TAG, "Fetching FCM token failed", task.getException());
                    }
                });

    }

    // Firebase Authentication - Sign in with email and password
    public void signInWithEmailAndPassword(String email, String password, final AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "signInWithEmail:success, User: " + user.getEmail());
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Firebase Authentication - Sign up with email and password
    public void createAccount(String email, String password, final AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "createAccount:success, User: " + user.getEmail());
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "createAccount:failure", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // Save user information in Firestore after authentication
    public void saveUserData(FirebaseUser user, String name, String phoneNumber) {
        if (user != null) {
            // Create a user document in Firestore
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("name", name);
            userMap.put("email", user.getEmail());
            userMap.put("phone", phoneNumber);
            userMap.put("createdAt", FieldValue.serverTimestamp());

            DocumentReference userRef = db.collection("users").document(user.getUid());
            userRef.set(userMap, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved successfully"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error saving user data", e));
        }
    }

    // Save arbitrary data in Firestore
    public void saveData(String collection, String documentId, Map<String, Object> data) {
         db.collection(collection).document(documentId).update(data)
                 .addOnSuccessListener(aVoid -> Log.d(TAG, "Data saved successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Error saving data", e));
    }

    // Fetch user data from Firestore
    public void getUserData(String sanitizedEmail, final FetchDataCallback callback) {
        db.collection("users").document(sanitizedEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            callback.onSuccess(document.getData()); // Pass the document data directly
                        } else {
                            Log.d(TAG, "No such document");
                            callback.onFailure("No such document");
                        }
                    } else {
                        Log.w(TAG, "Get failed with ", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }



    // Save links to Firebase Realtime Database
    public void saveLinks(String userId, ArrayList<String> links, final SaveLinksCallback callback) {
        if (userId != null && links != null) {
            databaseReference.child(userId).setValue(links)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Links saved successfully");
                            callback.onSuccess();
                        } else {
                            Log.w(TAG, "Error saving links", task.getException());
                            callback.onFailure(task.getException().getMessage());
                        }
                    });
        }
    }

    // Fetch links from Firebase Realtime Database
    // Fetch links from Firebase using email as the identifier
    public void fetchLinks(String sanitizedEmail, final FetchLinksCallback callback) {
        if (sanitizedEmail != null) {
            // Use the sanitized email to fetch the links
            db.collection("users")
                    .document(sanitizedEmail)  // Using sanitized email as document ID
                    .collection("links")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ArrayList<Link> links = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                String link = document.getString("link");
                                if (name != null && link != null) {
                                    links.add(new Link(name, link));  // Add both name and link to list
                                }
                            }
                            callback.onSuccess(links);  // Return the fetched links
                        } else {
                            callback.onFailure("Error fetching links: " + task.getException().getMessage());
                        }
                    });
        }
    }
//    // Send notification to users
//    public void sendNotificationToUser(String sanitizedEmail, String title, String message) {
//        db.collection("users").document(sanitizedEmail)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        DocumentSnapshot document = task.getResult();
//                        if (document.exists()) {
//                            String token = document.getString("fcmToken");
//                            if (token != null) {
//                                // Prepare the notification data
//                                Map<String, Object> notificationData = new HashMap<>();
//                                notificationData.put("to", token);
//                                Map<String, String> notificationBody = new HashMap<>();
//                                notificationBody.put("title", title);
//                                notificationBody.put("body", message);
//                                notificationData.put("notification", notificationBody);
//
//                                // Send the notification via FCM
//                                FirebaseFunctions.getInstance()
//                                        .getHttpsCallable("sendNotification")
//                                        .call(notificationData)
//                                        .addOnCompleteListener(notificationTask -> {
//                                            if (notificationTask.isSuccessful()) {
//                                                Log.d(TAG, "Notification sent successfully");
//                                            } else {
//                                                Log.w(TAG, "Error sending notification", notificationTask.getException());
//                                            }
//                                        });
//                            } else {
//                                Log.w(TAG, "No FCM token found for user: " + sanitizedEmail);
//                            }
//                        } else {
//                            Log.w(TAG, "User document does not exist: " + sanitizedEmail);
//                        }
//                    } else {
//                        Log.w(TAG, "Failed to retrieve user: " + task.getException());
//                    }
//                });
//    }

    public interface SaveTokenCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }
    // AuthCallback for success and failure responses
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    // FetchDataCallback for fetching user data
    // This annotation is optional but helps clarify the intention.
    public interface FetchDataCallback {
        void onSuccess(Map<String, Object> data); // Accepts a map of the document data
        void onFailure(String errorMessage);
    }


    // SaveLinksCallback for saving links
    public interface SaveLinksCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    // FetchLinksCallback for fetching links
    public interface FetchLinksCallback {
        void onSuccess(ArrayList<Link> links);
        void onFailure(String errorMessage);
    }
}
