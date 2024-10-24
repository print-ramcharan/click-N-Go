package com.example.clickngo.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.clickngo.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;
    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "settings_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, getContext().MODE_PRIVATE);

        // Set up theme switch
        boolean isDarkMode = getStoredThemeMode(); // Retrieve saved theme mode
        binding.switchTheme.setChecked(isDarkMode);

        // Apply saved theme when the fragment is created
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        binding.switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Apply dark theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                saveThemeMode(true); // Save the theme preference
            } else {
                // Apply light theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                saveThemeMode(false); // Save the theme preference
            }
        });

        // Set up list view customization
        binding.customizeListButton.setOnClickListener(v -> {
            // Open customization dialog
            CustomizationDialog dialog = new CustomizationDialog();
            dialog.show(getChildFragmentManager(), "customization_dialog");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clear the binding
    }

    // Save the theme mode (true for dark mode, false for light mode)
    private void saveThemeMode(boolean isDarkMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_THEME_MODE, isDarkMode);
        editor.apply();
    }

    // Retrieve the saved theme mode (default is false - light mode)
    private boolean getStoredThemeMode() {
        return sharedPreferences.getBoolean(KEY_THEME_MODE, false);
    }
}
