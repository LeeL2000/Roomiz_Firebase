package com.example.roomiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvProfileCards;  // RecyclerView for the profile cards
    private ProfileAdapter pa;            // Adapter for the profile cards
    private List<Profile> profiles;       // List of profiles
    private View endCard;                 // View for the end card
    private ProgressBar loadingSpinner;   // Loading indicator while Firestore fetches data
    private FirebaseAnalytics mFirebaseAnalytics;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());

        // Initializing views
        endCard = view.findViewById(R.id.endCard);
        rvProfileCards = view.findViewById(R.id.rvProfileCards);

        // Log screen view event to Analytics
        Bundle screenBundle = new Bundle();
        screenBundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "HomeFragment");
        screenBundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "HomeFragment");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenBundle);

        FirebaseCrashlytics.getInstance().log("HomeFragment loaded - fetching profiles from Firestore");

        // Load profiles from Firestore (dynamic data)
        ProfileRepository.getProfilesFromFirestore(new ProfileRepository.ProfilesCallback() {
            @Override
            public void onSuccess(List<Profile> loadedProfiles) {
                if (!isAdded()) return; // Guard against fragment detachment

                profiles = loadedProfiles;

                pa = new ProfileAdapter(profiles, new ProfileAdapter.OnActionListener() {
                    @Override
                    public void onLike(int position, View itemView) {
                        // Log like event to Firebase Analytics
                        String profileName = profiles.get(position).getName();
                        Bundle bundle = new Bundle();
                        bundle.putString("profile_name", profileName);
                        mFirebaseAnalytics.logEvent("profile_liked", bundle);
                        FirebaseCrashlytics.getInstance().log("User liked: " + profileName);

                        animateRightAndRemove(position, itemView);
                    }

                    @Override
                    public void onUnlike(int position, View itemView) {
                        // Log dislike event to Firebase Analytics
                        String profileName = profiles.get(position).getName();
                        Bundle bundle = new Bundle();
                        bundle.putString("profile_name", profileName);
                        mFirebaseAnalytics.logEvent("profile_disliked", bundle);
                        FirebaseCrashlytics.getInstance().log("User disliked: " + profileName);

                        animateLeftAndRemove(position, itemView);
                    }

                    @Override
                    public void onChat(Profile profile) {
                        // Log chat open event to Firebase Analytics
                        Bundle bundle = new Bundle();
                        bundle.putString("profile_name", profile.getName());
                        mFirebaseAnalytics.logEvent("chat_opened", bundle);
                        FirebaseCrashlytics.getInstance().log("User opened chat with: " + profile.getName());

                        openChatsFragment(profile.getName());
                    }
                });

                rvProfileCards.setLayoutManager(new StackLayoutManager());
                rvProfileCards.setAdapter(pa);

                // Log how many profiles were loaded
                Bundle loadBundle = new Bundle();
                loadBundle.putInt("profiles_count", profiles.size());
                mFirebaseAnalytics.logEvent("profiles_loaded", loadBundle);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                FirebaseCrashlytics.getInstance().recordException(e);
                Toast.makeText(requireContext(), "Failed to load profiles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatsFragment(String name) {
        ChatsFragment fragment = new ChatsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("search_name", name);
        fragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flHome, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void animateRightAndRemove(int position, View itemView) {
        String name = profiles.get(position).getName();
        showMatchToast("It's a match with " + name + "! 🎉");

        itemView.animate()
                .translationX(itemView.getWidth())
                .rotation(12f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    pa.removeCard(position);
                    checkIfFinished();
                })
                .start();
    }

    private void animateLeftAndRemove(int position, View itemView) {
        showMatchToast("Maybe next time");

        itemView.animate()
                .translationX(-itemView.getWidth())
                .rotation(-12f)
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    pa.removeCard(position);
                    checkIfFinished();
                })
                .start();
    }

    // Check if RecyclerView is empty and show end card if so
    private void checkIfFinished() {
        if (pa.getItemCount() == 0) {
            rvProfileCards.setVisibility(View.GONE);
            endCard.setVisibility(View.VISIBLE);

            // Log "all profiles viewed" event to Analytics
            mFirebaseAnalytics.logEvent("all_profiles_viewed", null);
            FirebaseCrashlytics.getInstance().log("User has viewed all profiles");
        }
    }

    // Show a custom "match" Toast
    private void showMatchToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View layout = inflater.inflate(R.layout.match_toast, null);
        TextView tv = layout.findViewById(R.id.tvMatchToast);
        tv.setText(message);

        Toast toast = new Toast(requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
