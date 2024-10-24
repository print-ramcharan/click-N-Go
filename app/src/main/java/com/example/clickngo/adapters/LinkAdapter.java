package com.example.clickngo.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.clickngo.R;
import com.example.clickngo.models.Link;

import java.util.ArrayList;

public class LinkAdapter extends ArrayAdapter<Link> {

    private SparseBooleanArray selectedItems;  // To keep track of selected items
    private boolean isMultiSelectMode = false; // Track multi-select mode state
    private SharedPreferences preferences;

    public LinkAdapter(Context context, ArrayList<Link> links) {
        super(context, 0, links);
        selectedItems = new SparseBooleanArray();
        preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Link link = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_link, parent, false);
        }

        TextView nameTextView = convertView.findViewById(R.id.nameTextView);
        TextView linkTextView = convertView.findViewById(R.id.linkTextView);
        CheckBox checkBox = convertView.findViewById(R.id.checkbox);

        nameTextView.setText(link.getName());
        linkTextView.setText(link.getLink());

        // Fetch customization settings from SharedPreferences
        String savedColor = preferences.getString("listview_color", "Default");
        String savedFontSize = preferences.getString("listview_font_size", "Medium");

        // Apply the color and font size based on preferences
        int textColor = getColorFromPreference(savedColor);
        int textSize = getFontSizeFromPreference(savedFontSize);

        // Apply color and font size dynamically
        nameTextView.setTextColor(textColor);
        linkTextView.setTextColor(textColor);
        nameTextView.setTextSize(textSize);
        linkTextView.setTextSize(textSize);

        // Checkboxes visibility based on multi-select mode
        if (isMultiSelectMode) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(selectedItems.get(position, false));  // Set the checked state
        } else {
            checkBox.setVisibility(View.GONE);  // Hide checkboxes when not in multi-select mode
        }

        // Handle checkbox click
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedItems.put(position, isChecked);  // Update selection state
        });

        return convertView;
    }

    // Convert color name to actual color value
    private int getColorFromPreference(String colorName) {
        switch (colorName) {
            case "Red":
                return Color.RED;
            case "Green":
                return Color.GREEN;
            case "Blue":
                return Color.BLUE;
            default:
                return Color.BLACK;  // Default color if no match
        }
    }

    // Convert font size name to actual size value
    private int getFontSizeFromPreference(String fontSizeName) {
        switch (fontSizeName) {
            case "Small":
                return 14;  // Small font size
            case "Large":
                return 20;  // Large font size
            default:
                return 16;  // Default (Medium) font size
        }
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    // Start multi-select mode
    public void startMultiSelectMode() {
        isMultiSelectMode = true;
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    // Toggle selection
    public void toggleSelection(int position) {
        if (isMultiSelectMode()) {
            if (selectedItems.get(position, false)) {
                selectedItems.delete(position);
            } else {
                selectedItems.put(position, true);
            }
            notifyDataSetChanged();
        }
    }

    // Get selected links
    public ArrayList<Link> getSelectedLinks() {
        ArrayList<Link> selectedLinks = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                selectedLinks.add(getItem(selectedItems.keyAt(i)));
            }
        }
        return selectedLinks;
    }

    public void shareSelectedLinks(Context context, String userName, String userEmail) {
        ArrayList<Link> selectedLinks = getSelectedLinks();

        if (!selectedLinks.isEmpty()) {
            StringBuilder linksText = new StringBuilder();

            // Add user details to the message
            linksText.append("Shared by: ").append(userName).append("\n");
            linksText.append("Email: ").append(userEmail).append("\n\n");

            // Add the links with names
            for (Link link : selectedLinks) {
                linksText.append("Name: ").append(link.getName()).append("\n");
                linksText.append("Link: ").append(link.getLink()).append("\n\n");
            }

            // Create the intent to share the content
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, linksText.toString());

            context.startActivity(Intent.createChooser(intent, "Share links"));
        }
    }

    public void setMultiSelectMode(boolean isMultiSelect) {
        this.isMultiSelectMode = isMultiSelect;
        notifyDataSetChanged();  // Refresh the list
    }
}
