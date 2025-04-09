package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "ReminderPrefs";
    private static final String TASKS_KEY = "saved_tasks";

    private TextView textTotalTasks;
    private TextView textCompletedTasks;
    private TextView textCompletionRate;
    private TextView textHighPriority;
    private TextView textMediumPriority;
    private TextView textLowPriority;
    private RecyclerView recyclerViewCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        textTotalTasks = findViewById(R.id.textTotalTasks);
        textCompletedTasks = findViewById(R.id.textCompletedTasks);
        textCompletionRate = findViewById(R.id.textCompletionRate);
        textHighPriority = findViewById(R.id.textHighPriority);
        textMediumPriority = findViewById(R.id.textMediumPriority);
        textLowPriority = findViewById(R.id.textLowPriority);
        recyclerViewCategories = findViewById(R.id.recyclerViewCategories);

        // Load and display statistics
        loadStatistics();
    }

    private void loadStatistics() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String tasksJson = sharedPreferences.getString(TASKS_KEY, "[]");

        try {
            JSONArray tasksArray = new JSONArray(tasksJson);
            int totalTasks = tasksArray.length();
            int completedTasks = 0;
            Map<Category, Integer> categoryCount = new HashMap<>();
            Map<TaskItem.Priority, Integer> priorityCount = new HashMap<>();

            // Initialize counts
            for (Category category : Category.values()) {
                categoryCount.put(category, 0);
            }
            priorityCount.put(TaskItem.Priority.LOW, 0);
            priorityCount.put(TaskItem.Priority.MEDIUM, 0);
            priorityCount.put(TaskItem.Priority.HIGH, 0);

            // Process tasks
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskObj = tasksArray.getJSONObject(i);
                
                // Count completed tasks
                if (taskObj.optBoolean("completed", false)) {
                    completedTasks++;
                }

                // Count categories
                String categoryStr = taskObj.optString("category", "OTHER");
                Category category = Category.valueOf(categoryStr);
                categoryCount.put(category, categoryCount.get(category) + 1);

                // Count priorities
                String priorityStr = taskObj.optString("priority", "MEDIUM");
                TaskItem.Priority priority = TaskItem.Priority.valueOf(priorityStr);
                priorityCount.put(priority, priorityCount.get(priority) + 1);
            }

            // Update UI
            updateOverallStats(totalTasks, completedTasks);
            updatePriorityStats(priorityCount);
            updateCategoryStats(categoryCount);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateOverallStats(int totalTasks, int completedTasks) {
        textTotalTasks.setText(totalTasks + "\nTotal Tasks");
        textCompletedTasks.setText(completedTasks + "\nCompleted");
        
        float completionRate = totalTasks > 0 ? (completedTasks * 100f / totalTasks) : 0;
        textCompletionRate.setText(String.format("%.1f%%\nCompletion Rate", completionRate));
    }

    private void updatePriorityStats(Map<TaskItem.Priority, Integer> priorityCount) {
        textHighPriority.setText("High Priority: " + priorityCount.get(TaskItem.Priority.HIGH) + " tasks");
        textMediumPriority.setText("Medium Priority: " + priorityCount.get(TaskItem.Priority.MEDIUM) + " tasks");
        textLowPriority.setText("Low Priority: " + priorityCount.get(TaskItem.Priority.LOW) + " tasks");
    }

    private void updateCategoryStats(Map<Category, Integer> categoryCount) {
        List<CategoryStat> categoryStats = new ArrayList<>();
        for (Map.Entry<Category, Integer> entry : categoryCount.entrySet()) {
            if (entry.getKey() != Category.ALL) {
                categoryStats.add(new CategoryStat(entry.getKey(), entry.getValue()));
            }
        }

        CategoryStatsAdapter adapter = new CategoryStatsAdapter(categoryStats);
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCategories.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 