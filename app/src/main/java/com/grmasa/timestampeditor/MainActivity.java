package com.grmasa.timestampeditor;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILES_REQUEST = 1;

    private final List<Uri> selectedFileUris = new ArrayList<>();
    private final Calendar selectedDate = Calendar.getInstance();
    private TextView filePathText, dateText;

    // Settings variables
    private String timestampFormat, fileTypeFilter;
    private boolean rootAccessEnabled;
    private ActivityResultLauncher<Intent> filePickerLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Objects.requireNonNull(getWindow().getInsetsController()).hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        filePathText = findViewById(R.id.filePathText);
        dateText = findViewById(R.id.dateText);
        Button selectFilesButton = findViewById(R.id.selectFilesButton);
        Button pickDateButton = findViewById(R.id.pickDateButton);
        Button applyButton = findViewById(R.id.applyButton);

        selectFilesButton.setOnClickListener(v -> selectFiles());
        pickDateButton.setOnClickListener(v -> pickDateTime());
        applyButton.setOnClickListener(v -> applyTimestampBatch());

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedFileUris.clear();
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                                selectedFileUris.add(data.getClipData().getItemAt(i).getUri());
                            }
                        } else if (data.getData() != null) {
                            selectedFileUris.add(data.getData());
                        }
                        filePathText.setText(getString(R.string.selected_files, selectedFileUris.size()));
                    }
                }
        );
        // Load settings
        loadSettings();
    }

    private void selectFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // Apply file type filter
        if ("PDF".equals(fileTypeFilter)) {
            intent.setType("application/pdf");
        } else if ("Images".equals(fileTypeFilter)) {
            intent.setType("image/*");
        } else if ("Text Files".equals(fileTypeFilter)) {
            intent.setType("text/*");
        } else if ("Audio".equals(fileTypeFilter)) {
            intent.setType("audio/*");
        } else if ("Videos".equals(fileTypeFilter)) {
            intent.setType("video/*");
        } else {
            intent.setType("*/*");
        }

        filePickerLauncher.launch(intent);
    }

    private void pickDateTime() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                updateDateText();
            }, selectedDate.get(Calendar.HOUR_OF_DAY), selectedDate.get(Calendar.MINUTE), true);

            timePicker.show();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void updateDateText() {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        dateText.setText(dateFormat.format(selectedDate.getTime()));
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(timestampFormat, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void applyTimestampBatch() {
        if (selectedFileUris.isEmpty()) {
            filePathText.setText(getString(R.string.no_files_selected));
            return;
        }

        for (Uri uri : selectedFileUris) {
            String path = FileUtil.getFullPathFromTreeUri(uri, this);
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    long originalTimestamp = file.lastModified();
                    String originalDate = formatDate(originalTimestamp);
                    // Get and format the updated last modified date
                    long updatedTimestamp = file.lastModified();
                    String updatedDate = formatDate(updatedTimestamp);

                    // Log the original date
                    System.out.println("Original date of file: " + path + " - " + originalDate);

                    boolean success = file.setLastModified(selectedDate.getTimeInMillis());
                    if (!success) {
                        if (rootAccessEnabled) {
                            applyWithRoot(path);
                        } else {
                            Toast.makeText(this,
                                    "Timestamp update may not work without root.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    System.out.println("Updated date of file: " + path + " - " + updatedDate);

                }
            }
        }
        filePathText.setText(getString(R.string.timestamps_updated));
    }

    private void applyWithRoot(String path) {
        try {
            Process su = Runtime.getRuntime().exec("su");
            String dateFormat = getString(R.string.touch_date_format);
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
            String formattedDate = sdf.format(selectedDate.getTime());
            String command = "touch -m -t " + formattedDate + " '" + path + "' && " +
                    "touch -a -t " + formattedDate + " '" + path + "'\nexit\n";
            su.getOutputStream().write(command.getBytes());
            su.getOutputStream().flush();
            su.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while applying timestamp with root" + e);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILES_REQUEST && resultCode == RESULT_OK) {
            selectedFileUris.clear();
            if (data != null) {
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        selectedFileUris.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    selectedFileUris.add(data.getData());
                }
            }
            filePathText.setText(getString(R.string.selected_files, selectedFileUris.size()));

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSettings() {
        // Load settings from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("TimestampEditorPrefs", MODE_PRIVATE);
        timestampFormat = sharedPreferences.getString("timestamp_format", "yyyy-MM-dd HH:mm:ss");
        rootAccessEnabled = sharedPreferences.getBoolean("root_access", false);
        fileTypeFilter = sharedPreferences.getString("file_type_filter", "All Files");
    }
}
