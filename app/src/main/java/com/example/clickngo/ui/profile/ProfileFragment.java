package com.example.clickngo.ui.profile;

import static com.example.clickngo.MainActivity.sanitizeEmail;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.clickngo.R;
import com.example.clickngo.databinding.FragmentProfileBinding;
import com.example.clickngo.models.UserProfile;
import com.example.clickngo.repositories.FirebaseRepo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class ProfileFragment extends Fragment {

    public interface ProfileUpdateListener {
        void onProfileUpdated();
    }


    private ProfileViewModel profileViewModel;
    private FragmentProfileBinding binding;
    private FirebaseRepo firebaseRepo;
    private FirebaseAuth mAuth;
    private static final int PICK_IMAGE_REQUEST = 1; // Request code for image picker
    private Uri selectedImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Initialize FirebaseRepo and FirebaseAuth
        firebaseRepo = new FirebaseRepo();
        mAuth = FirebaseAuth.getInstance();

        // Binding the layout
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Observe the user data from the ViewModel
        profileViewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            // Update UI with user data
            updateProfileUI(user);
        });

        // Set up the Edit/Save button to toggle between edit and save mode
        binding.editSaveButton.setOnClickListener(v -> {
            if (binding.profileUsername.isEnabled()) {
                saveProfileChanges((ProfileUpdateListener) getActivity());
                toggleEditing(false);
            } else {
                toggleEditing(true);
            }
        });

        // Set up the profile image button
        binding.profileImageButton.setOnClickListener(v -> {
            openImagePicker();
        });

        return root;
    }

    private void updateProfileUI(UserProfile user) {
        // Set user data in the UI
        binding.profileUsername.setText(user.getDisplayName());
        binding.profileBio.setText(user.getBio());
        binding.profileEmail.setText(user.getEmail());
        binding.profileJoinedDate.setText(user.getJoinedDate());

        // Check if Base64 image exists
        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
            try {
                Bitmap image = decodeBase64ToBitmap(user.getProfileImageBase64());
                // Set the Bitmap directly to the ImageView
                binding.profileImage.setImageBitmap(image);
            } catch (Exception e) {
                // If there's an error decoding Base64, fall back to the Google default image URL
                Glide.with(this).load(user.getProfileImageUrl()).into(binding.profileImage);
            }
        } else {
            // If Base64 doesn't exist, load the Google default image from the URL
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this).load(user.getProfileImageUrl()).into(binding.profileImage);
            } else {
                // Load default image if both Base64 and profileImageUrl are missing
                Glide.with(this).load(R.drawable.ic_menu_profile).into(binding.profileImage);
            }
        }
    }

    // Helper method to decode Base64 string to Bitmap
    public static Bitmap decodeBase64ToBitmap(String base64Str) throws IllegalArgumentException {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }


    private void toggleEditing(boolean enable) {
        // Enable or disable editing the profile
        binding.profileUsername.setEnabled(enable);
        binding.profileBio.setEnabled(enable);
        binding.profileImageButton.setEnabled(enable); // Enable/disable the image button
        binding.profileImageButton.setClickable(enable); // Make the ImageButton clickable only in edit mode

        if (enable) {
            binding.editSaveButton.setText("Save");
        } else {
            binding.editSaveButton.setText("Edit");
        }
    }

    // Convert image Uri to Base64
    private String convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            byte[] imageBytes = getBytes(inputStream);
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert InputStream to byte array
    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return byteArrayOutputStream.toByteArray();
    }

    // Modify the onActivityResult method to convert image to Base64
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Convert the selected image to Base64
            String base64Image = convertImageToBase64(selectedImageUri);

            if (base64Image != null) {
                // Set the selected image as the profile image preview
                Glide.with(this).load(selectedImageUri).into(binding.profileImage);
            }
        }
    }

    // Modify the saveProfileChanges method to include the Base64 image in the update
    private void saveProfileChanges(ProfileUpdateListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String newName = binding.profileUsername.getText().toString();
            String newBio = binding.profileBio.getText().toString();
            String email = currentUser.getEmail();
            String sanitizedEmail = sanitizeEmail(email);

            // Prepare updated user data
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("displayName", newName);
            updatedData.put("bio", newBio);

            // If a new profile image is selected, convert it to Base64 and add to the map
            if (selectedImageUri != null) {
                String base64Image = convertImageToBase64(selectedImageUri);
                updatedData.put("profileImageBase64", base64Image); // Save the Base64 image string
            }

            // Save the updated data in Firestore
            firebaseRepo.saveData("users", sanitizedEmail, updatedData);
                    listener.onProfileUpdated();

        }
    }

    private void openImagePicker() {
        // Open image picker intent
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
