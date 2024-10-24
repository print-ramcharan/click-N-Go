package com.example.clickngo.ui.home;

import static com.example.clickngo.MainActivity.sanitizeEmail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.clickngo.R;
import com.example.clickngo.adapters.LinkAdapter;
import com.example.clickngo.databinding.FragmentHomeBinding;
import com.example.clickngo.models.Link;
import com.example.clickngo.repositories.FirebaseRepo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ListView listViewLinks;
    private FirebaseRepo firebaseRepo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<Link> linksList;
    private LinkAdapter adapter;
    private boolean isMultiSelectMode = false; // Track multi-select mode state
    private EditText searchEditText;

    private ArrayList<Link> filteredLinksList;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setHasOptionsMenu(true);

        // Initialize Firebase repository
        firebaseRepo = new FirebaseRepo();

        // Initialize the ListView and links list
        listViewLinks = binding.listViewLinks;
        linksList = new ArrayList<>();
        adapter = new LinkAdapter(getContext(), linksList);
        listViewLinks.setAdapter(adapter);

        // Set up SwipeRefreshLayout to handle pull-to-refresh
        swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchLinksFromFirebase();  // Reload the links when user pulls to refresh
            }
        });

        // Fetch links from Firebase and populate the ListView
        fetchLinksFromFirebase();

        searchEditText = binding.searchEditText; // Reference the search EditText
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLinks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });


        // Set an item click listener to open the link in the browser or toggle selection in multi-select mode
        listViewLinks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
                if (isMultiSelectMode) {
                    // In multi-select mode, toggle checkbox selection
                    adapter.toggleSelection(position);  // Toggle selection for this link
                } else {
                    // Normal behavior: open the link in the browser
                    Link selectedLink = linksList.get(position);
                    String url = selectedLink.getLink();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
            }
        });

        // Long press to enable multi-select mode
        listViewLinks.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parentView, View view, int position, long id) {
                if (!isMultiSelectMode) {
                    // Toggle multi-select mode
                    isMultiSelectMode = true;
                    adapter.startMultiSelectMode();
                    adapter.notifyDataSetChanged();
                    // Show the menu
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            requireActivity().invalidateOptionsMenu(); // Redraw the menu
                        }
                    });
                    return true;
                }
                return false;
            }
        });

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Only show the menu when in multi-select mode
        if (isMultiSelectMode) {
            inflater.inflate(R.menu.menu_multi_select, menu); // Show menu when multi-select mode is active
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String sanitizedEmail = sanitizeEmail(currentUser.getEmail());

                // Fetch user details from Firestore
                firebaseRepo.getUserData(sanitizedEmail, new FirebaseRepo.FetchDataCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        if (data != null) {
                            String userName = (String) data.get("displayName");
                            String userEmail = (String) data.get("email");

                            // Pass only name and email to the adapter
                            adapter.shareSelectedLinks(getContext(), userName, userEmail);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // If multi-select mode is active, show the menu
        if (isMultiSelectMode) {
            menu.findItem(R.id.menu_share).setVisible(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Handle back press to exit multi-select mode
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isMultiSelectMode) {
                    // Exit multi-select mode when back is pressed
                    exitMultiSelectMode();
                } else {
                    // Allow default behavior if not in multi-select mode
                    requireActivity().onBackPressed();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void filterLinks(String query) {
//        filteredLinksList.clear();
        if (query.isEmpty()) {
            filteredLinksList.addAll(linksList); // If query is empty, show all links
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Link link : linksList) {
                if (link.getLink().toLowerCase().contains(lowerCaseQuery)) {
                    filteredLinksList.add(link); // Add matching links to filtered list
                }
            }
        }
        adapter.notifyDataSetChanged(); // Notify adapter of data change
    }
    // Fetch links from Firebase based on the current user
    private void fetchLinksFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String sanitizedEmail = sanitizeEmail(email);

            if (sanitizedEmail != null) {
                firebaseRepo.fetchLinks(sanitizedEmail, new FirebaseRepo.FetchLinksCallback() {
                    @Override
                    public void onSuccess(ArrayList<Link> links) {
                        linksList.clear();
                        linksList.addAll(links);
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(getContext(), "Failed to fetch links: " + errorMessage, Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }
    }

    // Function to exit multi-select mode
    // Function to exit multi-select mode
    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        adapter.clearSelection();  // Clear the selection
        adapter.setMultiSelectMode(false); // Turn off multi-select mode in the adapter
        adapter.notifyDataSetChanged();  // Refresh the list
        requireActivity().invalidateOptionsMenu();  // Hide the menu
    }

}
