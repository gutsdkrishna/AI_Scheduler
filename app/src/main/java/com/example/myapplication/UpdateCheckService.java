package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateCheckService extends Service {
    private static final String TAG = "UpdateCheckService";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/gutsdkrishna/AI_Scheduler/releases/latest";
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_AUTO_UPDATE = "auto_update_enabled";
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        checkForUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkForUpdates();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkForUpdates() {
        // Check if auto-update is enabled
        boolean autoUpdateEnabled = sharedPreferences.getBoolean(KEY_AUTO_UPDATE, true);
        if (!autoUpdateEnabled) {
            Log.d(TAG, "Auto-update is disabled, skipping update check");
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String latestVersion = jsonResponse.getString("tag_name").replace("v", "");
                    String currentVersion = BuildConfig.VERSION_NAME;

                    Log.d(TAG, "Latest version: " + latestVersion + ", Current version: " + currentVersion);

                    if (!latestVersion.equals(currentVersion)) {
                        // Notify user about update
                        Intent updateIntent = new Intent(this, MainActivity.class);
                        updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        updateIntent.putExtra("update_available", true);
                        updateIntent.putExtra("latest_version", latestVersion);
                        startActivity(updateIntent);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
} 