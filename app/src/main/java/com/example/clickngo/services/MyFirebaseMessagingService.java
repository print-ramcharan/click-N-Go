package com.example.clickngo.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.clickngo.MainActivity;
import com.example.clickngo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Handle the new token, send it to your server or save in Firestore
        Log.d(TAG, "New token: " + token);
        // Save the token in Firestore using the sanitized email as the identifier
        saveTokenToFirestore(token);
    }

    public static void saveTokenToFirestore(String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail(); // Assuming user is logged in

        if (email != null) {
            // Sanitize email by replacing invalid characters
            String sanitizedEmail = email.replace(".", "_");
            Log.d(TAG, "Sanitized Email: " + sanitizedEmail);

            db.collection("users").document(sanitizedEmail)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully for user: " + sanitizedEmail))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving token for user: " + sanitizedEmail, e);
                    });
        } else {
            Log.w(TAG, "User is not logged in, cannot save FCM token");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Received notification with title: " + title + " and body: " + body);
            showNotification(title, body);
        } else {
            Log.w(TAG, "Received message does not contain notification payload");
        }

        // Log the data payload if available
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }
    }

    private void showNotification(String title, String messageBody) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "clickngo"; // Set your channel ID
        Log.d(TAG, "Channel ID: " + channelId);

        // For Android 8.0 and above, create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Channel human-readable title", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created: " + channelId);
        } else {
            Log.d(TAG, "Notification channel is not needed for Android version " + Build.VERSION.SDK_INT);
        }

        // Create an intent to open your main activity when the notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // This flag ensures the activity is not duplicated

        // Use FLAG_IMMUTABLE for Android 12 (API 31) and higher
        // Use FLAG_UPDATE_CURRENT for older versions
        int pendingIntentFlags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags);

        // Check if notificationManager is null
        if (notificationManager != null) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher) // Replace with your own icon
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)  // Add the pending intent here
                    .setDefaults(NotificationCompat.DEFAULT_ALL);  // Optional: to add sound/vibration to the notification

            // If no image URL is provided, continue with the basic notification setup (as before)
            Log.d(TAG, "Notification created with title: " + title + " and body: " + messageBody);
            notificationManager.notify(0, notificationBuilder.build());
        } else {
            Log.e(TAG, "Notification Manager is null, cannot show notification");
        }
    }

    // Periodically refresh the FCM token
    @Override
    public void onCreate() {
        super.onCreate();
        // Refresh token every 6 hours
        refreshTokenPeriodically();
    }

    private void refreshTokenPeriodically() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(6 * 60 * 60 * 1000); // Sleep for 6 hours
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String newToken = task.getResult();
                            Log.d(TAG, "Refreshed token: " + newToken);
                            saveTokenToFirestore(newToken); // Save the new token
                        } else {
                            Log.e(TAG, "Failed to refresh token", task.getException());
                        }
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Token refresh interrupted", e);
                }
            }
        }).start();
    }
}
