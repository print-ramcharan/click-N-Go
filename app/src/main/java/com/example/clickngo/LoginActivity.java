package com.example.clickngo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clickngo.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** @noinspection ALL */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final int RC_SIGN_IN = 9001; // Request code for Google sign-in

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Google Sign-In Button click
        binding.googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    // Configure Google Sign-In
    private void signInWithGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Your web client ID from Firebase console
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Handle the sign-in result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...)
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Authenticate with Firebase using the Google account
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<com.google.firebase.auth.AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.auth.AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Authentication Successful!", Toast.LENGTH_SHORT).show();

                            // Create user profile in Firestore if not already created
                            createUserProfile(user);

                            // Redirect to MainActivity
                            redirectToMain();
                        } else {
                            // If sign in fails, display a message to the user
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Create user profile in Firestore if it doesn't exist
    private void createUserProfile(FirebaseUser user) {
        String email = user.getEmail();
        String sanitizedEmail = sanitizeEmail(email);

        // Check if the user profile already exists
        db.collection("users").document(sanitizedEmail).get()
                .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().exists()) {
                                // If the user profile does not exist, create a new one
                                Map<String, Object> userProfileData = new HashMap<>();
                                userProfileData.put("email", email);
                                userProfileData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "No Name");
                                userProfileData.put("profileImageUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                                userProfileData.put("bio", "A new user on the app!");
                                userProfileData.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : null);
                                userProfileData.put("signupDate", System.currentTimeMillis());

                                // Save this data in Firestore
                                db.collection("users").document(sanitizedEmail)
                                        .set(userProfileData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(LoginActivity.this, "User profile created successfully.", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(LoginActivity.this, "Error creating user profile", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    }
                });
    }

    // Sanitize email for Firestore document
    private String sanitizeEmail(String email) {
        return email.replaceAll("[.]", "_");
    }

    // Redirect to MainActivity if login is successful
    private void redirectToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close the login activity
    }
}
