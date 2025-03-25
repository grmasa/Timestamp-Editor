package com.grmasa.timestampeditor;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private boolean isRootEnabled;
    private CheckBox rootAccessCheckbox, keepHistoryCheckbox;
    private Spinner fileTypeSpinner;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Objects.requireNonNull(getWindow().getInsetsController()).hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // views
        rootAccessCheckbox = findViewById(R.id.rootAccessCheckbox);
        keepHistoryCheckbox = findViewById(R.id.keepHistoryCheckbox);
        fileTypeSpinner = findViewById(R.id.fileTypeSpinner);
        Button saveSettingsButton = findViewById(R.id.saveSettingsButton);

        rootAccessCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isRootEnabled && isChecked) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Test Root Access")
                        .setMessage("Do you want to test root access now?")
                        .setPositiveButton("Yes", (dialog, which) -> testRootAccess())
                        .setNegativeButton("No", (dialog, which) -> {
                        }).show();
            }
        });

        sharedPreferences = getSharedPreferences("TimestampEditorPrefs", MODE_PRIVATE);

        loadSettings();

        // Handle Save Settings button click
        saveSettingsButton.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(SettingsActivity.this, "Settings saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void testRootAccess() {
        try {
            Process su = Runtime.getRuntime().exec("su");
            su.getOutputStream().write("exit\n".getBytes());
            su.getOutputStream().flush();
            int result = su.waitFor();
            if (result == 0) {
                Toast.makeText(this, "Root access granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Root access denied!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Root access test failed!", Toast.LENGTH_SHORT).show();
        }
    }

    // Load saved settings from SharedPreferences
    private void loadSettings() {
        // Load the history toggle state
        boolean keepHistory = sharedPreferences.getBoolean("keep_history", false);
        keepHistoryCheckbox.setChecked(keepHistory);

        // Load root access toggle state
        isRootEnabled = sharedPreferences.getBoolean("root_access", false);
        rootAccessCheckbox.setChecked(isRootEnabled);

        // Load the file type filter
        String savedFileTypeFilter = sharedPreferences.getString("file_type_filter", "All Files");
        String[] fileTypes = getResources().getStringArray(R.array.file_types);
        for (int i = 0; i < fileTypes.length; i++) {
            if (savedFileTypeFilter.equals(fileTypes[i])) {
                fileTypeSpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save root access toggle
        boolean isRootEnabled = rootAccessCheckbox.isChecked();
        editor.putBoolean("root_access", isRootEnabled);

        // Save history toggle
        editor.putBoolean("keep_history", keepHistoryCheckbox.isChecked());

        // Save the selected file type filter
        String selectedFileTypeFilter = (String) fileTypeSpinner.getSelectedItem();
        editor.putString("file_type_filter", selectedFileTypeFilter);

        // Commit changes
        editor.apply();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back to previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
