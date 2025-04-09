package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "ReminderPrefs";
    private static final String TASKS_KEY = "saved_tasks";

    private TextView tvTotalTasks;
    private TextView tvCompletionRate;
    private TextView tvMostActiveCategory;
    private TextView tvMostActiveTime;
    private RecyclerView rvCategoryStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Initialize views
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvMostActiveCategory = findViewById(R.id.tvMostActiveCategory);
        tvMostActiveTime = findViewById(R.id.tvMostActiveTime);
        rvCategoryStats = findViewById(R.id.rvCategoryStats);

        // Load and analyze statistics
        loadStatistics();
    }

    private void loadStatistics() {
        List<TaskItem> tasks = loadTasksFromPreferences();
        
        // Calculate basic statistics
        int totalTasks = tasks.size();
        int completedTasks = 0;
        Map<Category, Integer> categoryCount = new HashMap<>();
        Map<Integer, Integer> hourCount = new HashMap<>();

        for (TaskItem task : tasks) {
            // Count completed tasks
            if (task.isCompleted()) {
                completedTasks++;
            }

            // Count tasks per category
            Category category = task.getCategory();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);

            // Count tasks per hour
            try {
                String[] timeParts = task.getTime().split(":");
                int hour = Integer.parseInt(timeParts[0]);
                hourCount.put(hour, hourCount.getOrDefault(hour, 0) + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Calculate completion rate
        float completionRate = totalTasks > 0 ? (float) completedTasks / totalTasks * 100 : 0;

        // Find most active category
        Category mostActiveCategory = Category.OTHER;
        int maxCategoryCount = 0;
        for (Map.Entry<Category, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() > maxCategoryCount) {
                maxCategoryCount = entry.getValue();
                mostActiveCategory = entry.getKey();
            }
        }

        // Find most active hour
        int mostActiveHour = 0;
        int maxHourCount = 0;
        for (Map.Entry<Integer, Integer> entry : hourCount.entrySet()) {
            if (entry.getValue() > maxHourCount) {
                maxHourCount = entry.getValue();
                mostActiveHour = entry.getKey();
            }
        }

        // Update UI
        tvTotalTasks.setText(String.format("Total Tasks: %d", totalTasks));
        tvCompletionRate.setText(String.format("Completion Rate: %.1f%%", completionRate));
        tvMostActiveCategory.setText(String.format("Most Active Category: %s", 
            mostActiveCategory.getDisplayName()));
        tvMostActiveTime.setText(String.format("Most Active Time: %02d:00", mostActiveHour));

        // Setup category statistics RecyclerView
        setupCategoryStats(categoryCount, totalTasks);
    }

    private void setupCategoryStats(Map<Category, Integer> categoryCount, int totalTasks) {
        List<CategoryStat> categoryStats = new ArrayList<>();
        for (Category category : Category.values()) {
            int count = categoryCount.getOrDefault(category, 0);
            float percentage = totalTasks > 0 ? (float) count / totalTasks * 100 : 0;
            categoryStats.add(new CategoryStat(category, count, percentage));
        }

        CategoryStatsAdapter adapter = new CategoryStatsAdapter(categoryStats);
        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryStats.setAdapter(adapter);
    }

    private List<TaskItem> loadTasksFromPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String tasksJson = prefs.getString(TASKS_KEY, "[]");
        List<TaskItem> tasks = new ArrayList<>();

        try {
            JSONArray tasksArray = new JSONArray(tasksJson);
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskObj = tasksArray.getJSONObject(i);
                String task = taskObj.getString("task");
                String time = taskObj.getString("time");
                Category category = Category.valueOf(taskObj.optString("category", "OTHER"));
                TaskItem taskItem = new TaskItem(task, time, category);
                taskItem.setCompleted(taskObj.optBoolean("completed", false));
                tasks.add(taskItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return tasks;
    }

    public static class CategoryStat {
        Category category;
        int count;
        float percentage;

        CategoryStat(Category category, int count, float percentage) {
            this.category = category;
            this.count = count;
            this.percentage = percentage;
        }
    }
} 