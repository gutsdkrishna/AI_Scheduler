package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_AUTO_UPDATE = "auto_update_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    
    private SharedPreferences sharedPreferences;
    private SwitchMaterial switchAutoUpdate;
    private SwitchMaterial switchDarkMode;
    private MaterialButton btnCheckUpdate;
    private TextView txtCurrentVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize UI elements
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        switchAutoUpdate = findViewById(R.id.switchAutoUpdate);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        txtCurrentVersion = findViewById(R.id.txtCurrentVersion);

        // Set up toolbar
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Set current version
        txtCurrentVersion.setText("Current version: " + BuildConfig.VERSION_NAME);

        // Load saved preferences
        boolean autoUpdateEnabled = sharedPreferences.getBoolean(KEY_AUTO_UPDATE, true);
        boolean darkModeEnabled = sharedPreferences.getBoolean(KEY_DARK_MODE, false);

        // Set initial states
        switchAutoUpdate.setChecked(autoUpdateEnabled);
        switchDarkMode.setChecked(darkModeEnabled);

        // Set listeners
        switchAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_AUTO_UPDATE, isChecked);
            editor.apply();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();
            
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Set up manual update check button
        btnCheckUpdate.setOnClickListener(v -> {
            Intent updateIntent = new Intent(this, UpdateCheckService.class);
            startService(updateIntent);
        });
    }
} 