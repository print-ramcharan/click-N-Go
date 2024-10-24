package com.example.clickngo.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.clickngo.R;
import com.example.clickngo.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private AboutViewModel aboutViewModel;
    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        aboutViewModel = new ViewModelProvider(this).get(AboutViewModel.class);

        // Set up the UI (if needed)
        aboutViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            // You can use this to update your UI with the ViewModel data
        });

        // Set developer website action
        binding.btnDeveloperWebsite.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://print-ramcharan.github.io/personal-portfolio/"));
            startActivity(browserIntent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clear the binding
    }
}
