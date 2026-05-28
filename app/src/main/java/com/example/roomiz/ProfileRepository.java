package com.example.roomiz;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileRepository {

    private static final String TAG = "ProfileRepository";
    private static final String COLLECTION_NAME = "profiles";

    // Callback interface for async Firestore loading
    public interface ProfilesCallback {
        void onSuccess(List<Profile> profiles);
        void onFailure(Exception e);
    }

    // Load profiles dynamically from Firebase Firestore
    public static void getProfilesFromFirestore(ProfilesCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseCrashlytics.getInstance().log("Loading profiles from Firestore");

        db.collection(COLLECTION_NAME)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Profile> profiles = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String name = document.getString("name");
                                Long ageLong = document.getLong("age");
                                Long matchLong = document.getLong("matchPercentage");
                                String city = document.getString("city");
                                String bio = document.getString("bio");
                                String hobby1 = document.getString("hobby1");
                                String hobby2 = document.getString("hobby2");
                                String imageName = document.getString("imageName");

                                int age = (ageLong != null) ? ageLong.intValue() : 0;
                                int matchPercentage = (matchLong != null) ? matchLong.intValue() : 0;
                                int imageRes = resolveDrawableByName(imageName);

                                Profile profile = new Profile(imageRes, name, age, city, matchPercentage, bio, hobby1, hobby2);
                                profiles.add(profile);
                                Log.d(TAG, "Loaded profile: " + name);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing profile: " + document.getId(), e);
                                FirebaseCrashlytics.getInstance().recordException(e);
                            }
                        }

                        if (profiles.isEmpty()) {
                            Log.w(TAG, "Firestore returned no profiles, using local fallback data");
                            FirebaseCrashlytics.getInstance().log("Firestore empty - using fallback");
                            callback.onSuccess(getLocalFallbackProfiles());
                        } else {
                            Log.d(TAG, "Successfully loaded " + profiles.size() + " profiles from Firestore");
                            callback.onSuccess(profiles);
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Failed to load profiles from Firestore", e);
                        if (e != null) FirebaseCrashlytics.getInstance().recordException(e);
                        Log.w(TAG, "Falling back to local profile data");
                        callback.onSuccess(getLocalFallbackProfiles());
                    }
                });
    }

    // Helper: resolve a drawable resource ID by the imageName string stored in Firestore
    private static int resolveDrawableByName(String imageName) {
        if (imageName == null) return R.drawable.mika_dan;
        switch (imageName) {
            case "mika_dan":    return R.drawable.mika_dan;
            case "gaya_refael": return R.drawable.gaya_refael;
            case "ori_keidar":  return R.drawable.ori_keidar;
            case "tom_sasson":  return R.drawable.tom_sasson;
            case "alon_ron":    return R.drawable.alon_ron;
            case "daniel_levy": return R.drawable.daniel_levy;
            default:            return R.drawable.mika_dan;
        }
    }

    // Local fallback profiles (used if Firestore is unavailable or empty)
    public static List<Profile> getLocalFallbackProfiles() {
        List<Profile> profiles = new ArrayList<>();

        profiles.add(new Profile(
                R.drawable.mika_dan, "Mika Dan", 24, "Haifa", 89,
                "I love photography, long walks by the beach, and trying new coffee places.",
                "📸 Photography", "☕ Coffee"
        ));

        profiles.add(new Profile(
                R.drawable.gaya_refael, "Gaya Refael", 26, "Jerusalem", 91,
                "I enjoy art museums, weekend trips, and cooking for friends and family.",
                "🎨 Art", "🚗 Travel"
        ));

        profiles.add(new Profile(
                R.drawable.daniel_levy, "Daniel Levy", 25, "Tel Aviv", 93,
                "I'm a student of communications, and in my free time, I enjoy cooking, traveling, and hanging out with friends.",
                "🎵 Pop, Hip-hop", "🏠 Apartment"
        ));

        return profiles;
    }
}
