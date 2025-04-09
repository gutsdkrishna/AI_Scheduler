package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.app.PendingIntent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.ArrayAdapter;

public class MainActivity extends AppCompatActivity {
    private static final int SPEECH_REQUEST_CODE = 123;
    private static final int PERMISSION_REQUEST_CODE = 456;
    private static final String PREFS_NAME = "ReminderPrefs";
    private static final String TASKS_KEY = "saved_tasks";
    
    private EditText inputTask;
    private MaterialButton btnParse;
    private MaterialButton btnVoiceInput;
    private RecyclerView recyclerViewTasks;
    private List<TaskItem> taskList;
    private TaskAdapter taskAdapter;
    private SpeechRecognizer speechRecognizer;
    private SharedPreferences sharedPreferences;
    private ChipGroup chipGroupCategory;
    private ChipGroup chipGroupRecurrence;
    private Category selectedCategory = Category.OTHER;
    private TaskItem.RecurrenceType selectedRecurrence = TaskItem.RecurrenceType.NONE;
    private int nextRecurrenceId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Start update check service
        startService(new Intent(this, UpdateCheckService.class));

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Request notification permission on Android 13 and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }

        // Initialize UI elements
        inputTask = findViewById(R.id.inputTask);
        btnParse = findViewById(R.id.btnParse);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        chipGroupRecurrence = findViewById(R.id.chipGroupRecurrence);

        // Setup RecyclerView
        taskList = new ArrayList<>();
        setupRecyclerView();

        // Load saved tasks
        loadSavedTasks();

        // Check if we need to reschedule reminders after reboot
        if (getIntent() != null && "RESCHEDULE_REMINDERS".equals(getIntent().getAction())) {
            String tasksJson = getIntent().getStringExtra("tasks");
            if (tasksJson != null) {
                rescheduleRemindersFromJson(tasksJson);
            }
        }

        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    inputTask.setText(spokenText);
                }
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
                btnVoiceInput.setEnabled(false);
                Toast.makeText(MainActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int error) {
                btnVoiceInput.setEnabled(true);
            }

            @Override
            public void onEndOfSpeech() {
                btnVoiceInput.setEnabled(true);
            }

            // Required but unused methods
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        // Set click listener for the voice input button
        btnVoiceInput.setOnClickListener(v -> startVoiceInput());

        // Set click listener for the parse button
        btnParse.setOnClickListener(v -> {
            String userInput = inputTask.getText().toString().trim();
            if (!userInput.isEmpty()) {
                sendToNebiusAPI(userInput);
            } else {
                Toast.makeText(this, "Please enter a reminder", Toast.LENGTH_SHORT).show();
            }
        });

        setupCategoryChips();
        setupRecurrenceChips();

        MaterialButton btnStatistics = findViewById(R.id.btnStatistics);
        btnStatistics.setOnClickListener(v -> {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecurrenceChips() {
        for (TaskItem.RecurrenceType recurrenceType : TaskItem.RecurrenceType.values()) {
            Chip chip = new Chip(this);
            chip.setText(getRecurrenceDisplayName(recurrenceType));
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            
            // Set chip color
            int color = ContextCompat.getColor(this, R.color.purple_500);
            chip.setChipBackgroundColor(ColorStateList.valueOf(color));
            chip.setTextColor(Color.WHITE);
            
            chipGroupRecurrence.addView(chip);

            if (recurrenceType == TaskItem.RecurrenceType.NONE) {
                chip.setChecked(true);
            }
        }

        chipGroupRecurrence.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                String recurrenceName = chip.getText().toString();
                for (TaskItem.RecurrenceType recurrenceType : TaskItem.RecurrenceType.values()) {
                    if (getRecurrenceDisplayName(recurrenceType).equals(recurrenceName)) {
                        selectedRecurrence = recurrenceType;
                        break;
                    }
                }
            }
        });
    }

    private String getRecurrenceDisplayName(TaskItem.RecurrenceType recurrenceType) {
        switch (recurrenceType) {
            case NONE:
                return "One-time";
            case DAILY:
                return "Daily";
            case WEEKLY:
                return "Weekly";
            case MONTHLY:
                return "Monthly";
            default:
                return "Unknown";
        }
    }

    private void loadSavedTasks() {
        String tasksJson = sharedPreferences.getString(TASKS_KEY, "[]");
        try {
            JSONArray tasksArray = new JSONArray(tasksJson);
            List<TaskItem> savedTasks = new ArrayList<>();

            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskObj = tasksArray.getJSONObject(i);
                String task = taskObj.getString("task");
                String time = taskObj.getString("time");
                String categoryStr = taskObj.optString("category", "OTHER");
                Category category = Category.valueOf(categoryStr);
                String recurrenceStr = taskObj.optString("recurrence", "NONE");
                TaskItem.RecurrenceType recurrenceType = TaskItem.RecurrenceType.valueOf(recurrenceStr);
                boolean completed = taskObj.optBoolean("completed", false);
                int recurrenceId = taskObj.optInt("recurrenceId", 0);
                
                TaskItem taskItem = new TaskItem(task, time, category, recurrenceType);
                taskItem.setCompleted(completed);
                taskItem.setRecurrenceId(recurrenceId);
                
                savedTasks.add(taskItem);
                
                // Update nextRecurrenceId if needed
                if (recurrenceId >= nextRecurrenceId) {
                    nextRecurrenceId = recurrenceId + 1;
                }
            }

            taskAdapter.updateTasks(savedTasks);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveTasks() {
        try {
            JSONArray tasksArray = new JSONArray();
            for (TaskItem task : taskList) {
                JSONObject taskObj = new JSONObject();
                taskObj.put("task", task.getTask());
                taskObj.put("time", task.getTime());
                taskObj.put("category", task.getCategory().name());
                taskObj.put("completed", task.isCompleted());
                taskObj.put("recurrence", task.getRecurrenceType().name());
                taskObj.put("recurrenceId", task.getRecurrenceId());
                tasksArray.put(taskObj);
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(TASKS_KEY, tasksArray.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void rescheduleRemindersFromJson(String tasksJson) {
        try {
            JSONArray tasksArray = new JSONArray(tasksJson);
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskObj = tasksArray.getJSONObject(i);
                String task = taskObj.getString("task");
                String time = taskObj.getString("time");
                String categoryStr = taskObj.optString("category", "OTHER");
                Category category = Category.valueOf(categoryStr);
                String recurrenceStr = taskObj.optString("recurrence", "NONE");
                TaskItem.RecurrenceType recurrenceType = TaskItem.RecurrenceType.valueOf(recurrenceStr);
                
                TaskItem taskItem = new TaskItem(task, time, category, recurrenceType);
                scheduleReminder(taskItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognizer.startListening(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with voice input
                startVoiceInput();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void scheduleReminder(TaskItem task) {
        try {
            android.util.Log.d("MainActivity", "Scheduling reminder for task: " + task.getTask() + " at time: " + task.getTime());
            
            // Check for SCHEDULE_EXACT_ALARM permission on Android 12 and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    android.util.Log.d("MainActivity", "Requesting SCHEDULE_EXACT_ALARM permission");
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    return;
                }
            }

            // Parse the time string (HH:mm) into hours and minutes
            String[] timeParts = task.getTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Get current time
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            java.util.Calendar now = java.util.Calendar.getInstance();

            // Set the reminder time
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
            calendar.set(java.util.Calendar.MINUTE, minute);
            calendar.set(java.util.Calendar.SECOND, 0);

            // If the time has already passed today, schedule for tomorrow
            if (calendar.before(now)) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                android.util.Log.d("MainActivity", "Time has passed today, scheduling for tomorrow");
            }

            android.util.Log.d("MainActivity", "Scheduling for: " + calendar.getTime().toString());

            // Create a unique request code for each reminder
            int requestCode = (task.getTask() + task.getTime()).hashCode();
            
            // Set recurrence ID if not already set
            if (task.getRecurrenceId() == 0) {
                task.setRecurrenceId(nextRecurrenceId++);
            }
            
            // Schedule the main reminder
            scheduleAlarm(calendar.getTimeInMillis(), task, false, requestCode);
            android.util.Log.d("MainActivity", "Main reminder scheduled for: " + task.getTime());
            
            // Schedule the pre-notification (1 minute before)
            java.util.Calendar preCalendar = (java.util.Calendar) calendar.clone();
            preCalendar.add(java.util.Calendar.MINUTE, -1);
            
            // Only schedule pre-notification if it's in the future
            if (preCalendar.after(now)) {
                scheduleAlarm(preCalendar.getTimeInMillis(), task, true, requestCode + 1);
                android.util.Log.d("MainActivity", "Pre-notification scheduled for: " + 
                    preCalendar.get(java.util.Calendar.HOUR_OF_DAY) + ":" + 
                    preCalendar.get(java.util.Calendar.MINUTE));
            } else {
                android.util.Log.d("MainActivity", "Pre-notification time has already passed, not scheduling");
            }

            // Show success message
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, 
                    "Reminder scheduled for " + task.getTime(), 
                    Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("MainActivity", "Error scheduling reminder: " + e.getMessage());
            final String errorMessage = "Error scheduling reminder: " + e.getMessage();
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void scheduleAlarm(long timeInMillis, TaskItem task, boolean isPreNotification, int requestCode) {
        android.util.Log.d("MainActivity", "Creating alarm intent for task: " + task.getTask());
        
        // Create an intent for the alarm
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("task", task.getTask());
        intent.putExtra("time", task.getTime());
        intent.putExtra("is_pre_notification", isPreNotification);
        intent.putExtra("recurrence_type", task.getRecurrenceType().name());
        intent.putExtra("recurrence_id", task.getRecurrenceId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get the AlarmManager service
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) 
            getSystemService(Context.ALARM_SERVICE);

        // Schedule the alarm
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.util.Log.d("MainActivity", "Using setExactAndAllowWhileIdle for Android M and above");
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        } else {
            android.util.Log.d("MainActivity", "Using setExact for older Android versions");
            alarmManager.setExact(
                android.app.AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        }
        
        android.util.Log.d("MainActivity", "Alarm scheduled successfully for: " + new java.util.Date(timeInMillis).toString());
    }

    private void sendToNebiusAPI(String userInput) {
        new Thread(() -> {
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL("https://api.studio.nebius.com/v1/chat/completions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + BuildConfig.NEBIUS_API_KEY);
                connection.setDoOutput(true);

                // Create the request body with system prompt
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "deepseek-ai/DeepSeek-V3-0324-fast");
                requestBody.put("max_tokens", 512);
                requestBody.put("temperature", 0.3);
                requestBody.put("top_p", 0.95);

                JSONArray messages = new JSONArray();
                
                // Add system message
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", "You are a reminder parser. When given a natural language input about reminders, " +
                    "you must respond with ONLY a JSON array of objects, where each object has 'task', 'time', and 'category' fields. " +
                    "The time should be in 24-hour format (HH:mm). The category should be one of: HEALTH, WORK, PERSONAL, SHOPPING, STUDY, NORMAL, or OTHER. " +
                    "Use STUDY for academic tasks, homework, and learning activities. " +
                    "Use NORMAL for daily routine tasks like brushing teeth, eating meals, etc. " +
                    "Do not include any other text or formatting. " +
                    "Example input: 'Remind me to call mom at 8 PM, take medicine at 9 PM, and do homework at 10 PM' " +
                    "Example response: [{\"task\":\"call mom\",\"time\":\"20:00\",\"category\":\"PERSONAL\"},{\"task\":\"take medicine\",\"time\":\"21:00\",\"category\":\"HEALTH\"},{\"task\":\"do homework\",\"time\":\"22:00\",\"category\":\"STUDY\"}] " +
                    "Remember: Only return the JSON array, no other text or formatting.");
                messages.put(systemMessage);
                
                // Add user message
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", userInput);
                messages.put(userMessage);
                
                requestBody.put("messages", messages);

                // Send the request
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read the response
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                // Log the raw response for debugging
                System.out.println("Raw API Response: " + response.toString());

                // Parse the response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    final String contentText = firstChoice.getJSONObject("message").getString("content");
                    
                    // Log the content text for debugging
                    System.out.println("Content Text: " + contentText);
                    
                    // Clean the content text - remove markdown code blocks and trim
                    String cleanedContent = contentText.replace("```json", "")
                                           .replace("```", "")
                                           .trim();
                    
                    // Log the cleaned content for debugging
                    System.out.println("Cleaned Content: " + cleanedContent);
                    
                    try {
                        // Parse the cleaned content as JSON array
                        JSONArray tasksArray = new JSONArray(cleanedContent);
                        List<TaskItem> parsedTasks = new ArrayList<>();
                        
                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskObj = tasksArray.getJSONObject(i);
                            String task = taskObj.getString("task");
                            String time = taskObj.getString("time");
                            String categoryStr = taskObj.optString("category", "OTHER");
                            Category category = Category.valueOf(categoryStr);
                            TaskItem taskItem = new TaskItem(task, time, category, selectedRecurrence);
                            parsedTasks.add(taskItem);
                            
                            // Schedule the reminder
                            scheduleReminder(taskItem);
                        }

                        // Update UI on the main thread
                        runOnUiThread(() -> {
                            taskAdapter.updateTasks(parsedTasks);
                            saveTasks();
                            Toast.makeText(MainActivity.this, 
                                "Successfully scheduled " + parsedTasks.size() + " reminders", 
                                Toast.LENGTH_SHORT).show();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        final String finalContent = cleanedContent;
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                "Error parsing tasks: " + e.getMessage() + "\nContent: " + finalContent, 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        "Error: " + e.getMessage() + "\nResponse: " + response.toString(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setupCategoryChips() {
        for (Category category : Category.values()) {
            Chip chip = new Chip(this);
            chip.setText(category.getDisplayName());
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            
            // Set chip color based on category
            int color = ContextCompat.getColor(this, category.getColorResId());
            chip.setChipBackgroundColor(ColorStateList.valueOf(color));
            chip.setTextColor(Color.WHITE);
            
            chipGroupCategory.addView(chip);

            if (category == Category.ALL) {
                chip.setChecked(true);
            }
        }

        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                String categoryName = chip.getText().toString();
                for (Category category : Category.values()) {
                    if (category.getDisplayName().equals(categoryName)) {
                        selectedCategory = category;
                        taskAdapter.setFilter(category);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText editTextTask = dialogView.findViewById(R.id.editTextTask);
        EditText editTextTime = dialogView.findViewById(R.id.editTextTime);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        RadioGroup radioGroupPriority = dialogView.findViewById(R.id.radioGroupPriority);

        // Set up category spinner
        ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Category.values());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        builder.setTitle("Add Task")
                .setPositiveButton("Add", (dialog, which) -> {
                    String task = editTextTask.getText().toString();
                    String time = editTextTime.getText().toString();
                    Category category = (Category) spinnerCategory.getSelectedItem();
                    
                    // Get selected priority
                    TaskItem.Priority priority = TaskItem.Priority.MEDIUM; // Default
                    int selectedPriorityId = radioGroupPriority.getCheckedRadioButtonId();
                    if (selectedPriorityId == R.id.radioLow) {
                        priority = TaskItem.Priority.LOW;
                    } else if (selectedPriorityId == R.id.radioHigh) {
                        priority = TaskItem.Priority.HIGH;
                    }

                    if (!task.isEmpty() && !time.isEmpty()) {
                        TaskItem newTask = new TaskItem(task, time, category, TaskItem.RecurrenceType.NONE, priority);
                        taskList.add(newTask);
                        taskAdapter.notifyDataSetChanged();
                        saveTasks();
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, taskList);
        recyclerViewTasks.setAdapter(taskAdapter);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));

        // Add click listener for task items
        taskAdapter.setOnItemClickListener(position -> {
            TaskItem task = taskList.get(position);
            showEditTaskDialog(task, position);
        });
    }

    private void showEditTaskDialog(TaskItem task, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText editTextTask = dialogView.findViewById(R.id.editTextTask);
        EditText editTextTime = dialogView.findViewById(R.id.editTextTime);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        RadioGroup radioGroupPriority = dialogView.findViewById(R.id.radioGroupPriority);

        // Set up category spinner
        ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Category.values());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Set current values
        editTextTask.setText(task.getTask());
        editTextTime.setText(task.getTime());
        spinnerCategory.setSelection(task.getCategory().ordinal());
        
        // Set priority
        switch (task.getPriority()) {
            case LOW:
                radioGroupPriority.check(R.id.radioLow);
                break;
            case HIGH:
                radioGroupPriority.check(R.id.radioHigh);
                break;
            default:
                radioGroupPriority.check(R.id.radioMedium);
                break;
        }

        builder.setTitle("Edit Task")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTask = editTextTask.getText().toString();
                    String newTime = editTextTime.getText().toString();
                    Category newCategory = (Category) spinnerCategory.getSelectedItem();
                    
                    // Get selected priority
                    TaskItem.Priority newPriority = TaskItem.Priority.MEDIUM; // Default
                    int selectedPriorityId = radioGroupPriority.getCheckedRadioButtonId();
                    if (selectedPriorityId == R.id.radioLow) {
                        newPriority = TaskItem.Priority.LOW;
                    } else if (selectedPriorityId == R.id.radioHigh) {
                        newPriority = TaskItem.Priority.HIGH;
                    }

                    if (!newTask.isEmpty() && !newTime.isEmpty()) {
                        // Create a new task with updated values
                        TaskItem updatedTask = new TaskItem(newTask, newTime, newCategory, 
                                task.getRecurrenceType(), newPriority);
                        updatedTask.setCompleted(task.isCompleted());
                        updatedTask.setRecurrenceId(task.getRecurrenceId());
                        
                        // Replace the old task with the updated one
                        taskList.set(position, updatedTask);
                        taskAdapter.notifyItemChanged(position);
                        saveTasks();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    taskList.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    saveTasks();
                });

        builder.create().show();
    }
}

// TaskAdapter for RecyclerView
class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<TaskItem> taskList;
    private List<TaskItem> filteredList;
    private MainActivity mainActivity;
    private Category currentFilter = Category.ALL;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TaskAdapter(MainActivity mainActivity, List<TaskItem> taskList) {
        this.taskList = taskList;
        this.filteredList = new ArrayList<>(taskList);
        this.mainActivity = mainActivity;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        holder.bind(filteredList.get(position));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateTasks(List<TaskItem> newTasks) {
        taskList.addAll(newTasks);
        applyFilter(currentFilter);
        notifyDataSetChanged();
        mainActivity.saveTasks();
    }

    public void removeTask(int position) {
        TaskItem removedTask = filteredList.get(position);
        taskList.remove(removedTask);
        filteredList.remove(position);
        notifyItemRemoved(position);
        mainActivity.saveTasks();
    }

    public void setFilter(Category category) {
        currentFilter = category;
        applyFilter(category);
        notifyDataSetChanged();
    }

    private void applyFilter(Category category) {
        if (category == Category.ALL) {
            filteredList = new ArrayList<>(taskList);
        } else {
            filteredList = new ArrayList<>();
            for (TaskItem task : taskList) {
                if (task.getCategory() == category) {
                    filteredList.add(task);
                }
            }
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTask;
        private final TextView textTime;
        private final MaterialCardView cardView;
        private final MaterialButton btnDelete;
        private final CheckBox checkBoxComplete;
        private final TaskAdapter adapter;

        TaskViewHolder(View itemView, TaskAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            cardView = (MaterialCardView) itemView;
            textTask = itemView.findViewById(R.id.textTask);
            textTime = itemView.findViewById(R.id.textTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            checkBoxComplete = itemView.findViewById(R.id.checkBoxComplete);
            
            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && adapter.listener != null) {
                    adapter.listener.onItemClick(position);
                }
            });
        }

        void bind(TaskItem task) {
            textTask.setText(task.getTask());
            textTime.setText(task.getTime());
            
            // Set card stroke color based on category
            int color = ContextCompat.getColor(itemView.getContext(), task.getCategory().getColorResId());
            cardView.setStrokeColor(color);
            cardView.setStrokeWidth(2);
            
            // Set checkbox state
            checkBoxComplete.setChecked(task.isCompleted());
            
            // Apply strikethrough if completed
            if (task.isCompleted()) {
                textTask.setPaintFlags(textTask.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textTime.setPaintFlags(textTime.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textTask.setPaintFlags(textTask.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                textTime.setPaintFlags(textTime.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
            
            // Set checkbox listener
            checkBoxComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                // Use post to ensure this runs after the current layout operation
                buttonView.post(() -> {
                    adapter.notifyItemChanged(getAdapterPosition());
                    adapter.mainActivity.saveTasks();
                });
            });
            
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    adapter.removeTask(position);
                }
            });
        }
    }
}

