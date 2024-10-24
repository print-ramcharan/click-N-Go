package com.example.clickngo.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import com.example.clickngo.R;

public class CustomizationDialog extends AppCompatDialogFragment {

    private SharedPreferences sharedPreferences;
    private String[] colors = {"Default", "Red", "Green", "Blue"};
    private String[] fontSizes = {"Small", "Medium", "Large"};

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Customize ListView");

        // Create a layout for customization
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_customize_listview, null);

        // Initialize shared preferences to save customization settings
        sharedPreferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        // Spinner for list item background color
        Spinner colorSpinner = view.findViewById(R.id.color_spinner);
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, colors);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);

        // Spinner for font size
        Spinner fontSizeSpinner = view.findViewById(R.id.font_size_spinner);
        ArrayAdapter<String> fontSizeAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, fontSizes);
        fontSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSizeSpinner.setAdapter(fontSizeAdapter);

        // Set the default values based on SharedPreferences
        String savedColor = sharedPreferences.getString("listview_color", "Default");
        String savedFontSize = sharedPreferences.getString("listview_font_size", "Medium");

        // Set the previously selected values in spinners
        colorSpinner.setSelection(getIndex(colors, savedColor));
        fontSizeSpinner.setSelection(getIndex(fontSizes, savedFontSize));

        builder.setView(view)
                .setPositiveButton("Apply", (dialog, which) -> {
                    // Save the selected color and font size to SharedPreferences
                    String selectedColor = colorSpinner.getSelectedItem().toString();
                    String selectedFontSize = fontSizeSpinner.getSelectedItem().toString();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("listview_color", selectedColor);
                    editor.putString("listview_font_size", selectedFontSize);
                    editor.apply();

                    // Apply the changes (you could call a method in the activity to update the ListView)
                    Toast.makeText(getActivity(), "Customization applied!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                });

        return builder.create();
    }

    // Helper method to get the index of a value in an array
    private int getIndex(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return 0; // Default to 0 if not found
    }
}
