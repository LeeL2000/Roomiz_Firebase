package com.example.roomiz;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class RoomizApp extends Application {

    private static final String TAG = "RoomizApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Enable Crashlytics data collection
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        // Set custom keys for Crashlytics (useful for debugging)
        FirebaseCrashlytics.getInstance().setCustomKey("app_version", "1.0");
        FirebaseCrashlytics.getInstance().setCustomKey("environment", "production");

        // Log app start to Crashlytics
        FirebaseCrashlytics.getInstance().log("Roomiz app started");

        Log.d(TAG, "Firebase initialized successfully");
    }
}
