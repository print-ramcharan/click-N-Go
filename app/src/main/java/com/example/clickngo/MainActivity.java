package com.example.clickngo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.example.clickngo.databinding.ActivityMainBinding;
import com.example.clickngo.ui.profile.ProfileFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity  implements ProfileFragment.ProfileUpdateListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity"; // Tag for logging
    private FirebaseFirestore db;

    // SharedPreferences keys
    private static final String PREFS_NAME = "settings_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this); // Make sure Firebase is initialized
        requestNotificationPermission();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadUserData(currentUser);



        // Retrieve the saved theme preference and apply it
        applyThemeFromPreferences();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view -> {
            // Show a dialog to add a new link
            showAddLinkDialog();
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_settings, R.id.nav_about)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }

    @Override
    public void onProfileUpdated() {
        // Reload the user data after updating the profile
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserData(currentUser);
        }
    }
    // Method to apply the theme from SharedPreferences
    private void applyThemeFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_THEME_MODE, false); // Default is light mode

        if (isDarkMode) {
            // Apply dark theme
            setTheme(R.style.DarkTheme);  // Use the custom DarkTheme
            saveThemeMode(true);
        } else {
            // Apply light theme
            setTheme(R.style.LightTheme);  // Use the custom LightTheme
            saveThemeMode(false);
        }
    }


    // Save the theme mode in SharedPreferences
    private void saveThemeMode(boolean isDarkMode) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_THEME_MODE, isDarkMode);
        editor.apply();
    }

    // Method to redirect to the login activity
    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close the current activity so user can't navigate back
    }

    // Load user data
    public  void loadUserData(FirebaseUser currentUser) {
        Log.d(TAG, "Loading user data for: " + currentUser.getEmail());
        String sanitizedEmail = sanitizeEmail(currentUser.getEmail());
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Retrieve the user data from Firestore
        db.collection("users").document(sanitizedEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String displayName = documentSnapshot.getString("displayName");
                        String email = documentSnapshot.getString("email");
                        String bio = documentSnapshot.getString("bio");
                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64"); // Base64 string
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl"); // Image URL

                        NavigationView navigationView = binding.navView;
                        if (navigationView != null) {
                            View headerView = navigationView.getHeaderView(0);
                            TextView usernameTextView = headerView.findViewById(R.id.nav_header_username);
                            TextView emailTextView = headerView.findViewById(R.id.nav_header_email);

                            usernameTextView.setText(displayName != null ? displayName : "No name available");
                            emailTextView.setText(email != null ? email : "No email available");

                            ImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

                            // Check if profileImageBase64 exists and load it
                            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                Bitmap decodedImage = decodeBase64ToBitmap(profileImageBase64);
                                profileImageView.setImageBitmap(decodedImage);
                            }
                            // If profileImageBase64 does not exist, check if profileImageUrl exists and load it
                            else if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profileImageUrl)  // Load image using URL
                                        .placeholder(R.drawable.ic_menu_profile)
                                        .error(R.drawable.error)
                                        .into(profileImageView);
                            }
                            // If neither exists, load the default icon
                            else {
                                profileImageView.setImageResource(R.mipmap.ic_launcher_round);
                            }
                        }
                    } else {
                        Log.d(TAG, "User data does not exist in Firestore for: " + sanitizedEmail);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private Bitmap decodeBase64ToBitmap(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        } else {
            loadUserData(currentUser);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private void showAddLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add a New Link");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Enter the name here");

        final EditText urlInput = new EditText(this);
        urlInput.setHint("Enter the URL here");

        layout.addView(nameInput);
        layout.addView(urlInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String newLink = urlInput.getText().toString().trim();

            if (!name.isEmpty() && !newLink.isEmpty()) {
                saveLinkToFirebase(name, newLink);
            } else {
                Snackbar.make(binding.getRoot(), "Name and Link cannot be empty", Snackbar.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveLinkToFirebase(String name, String link) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null) {
                String sanitizedEmail = sanitizeEmail(email);
                Map<String, Object> linkData = new HashMap<>();
                linkData.put("name", name);
                linkData.put("link", link);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .document(sanitizedEmail)
                        .collection("links")
                        .add(linkData)
                        .addOnSuccessListener(documentReference -> {
                            Snackbar.make(binding.getRoot(), "Link added", Snackbar.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Snackbar.make(binding.getRoot(), "Error adding link", Snackbar.LENGTH_SHORT).show();
                        });
            }
        }
    }

    public static String sanitizeEmail(String email) {
        return email.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
    }
}
