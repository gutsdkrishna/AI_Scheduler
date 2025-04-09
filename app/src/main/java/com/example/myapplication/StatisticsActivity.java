package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;

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
    private TextView tvAverageTasksPerDay;
    private TextView tvMostProductiveDay;
    private RecyclerView rvCategoryStats;
    private LinearLayout weeklyChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Initialize views
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvMostActiveCategory = findViewById(R.id.tvMostActiveCategory);
        tvMostActiveTime = findViewById(R.id.tvMostActiveTime);
        tvAverageTasksPerDay = findViewById(R.id.tvAverageTasksPerDay);
        tvMostProductiveDay = findViewById(R.id.tvMostProductiveDay);
        rvCategoryStats = findViewById(R.id.rvCategoryStats);
        weeklyChart = findViewById(R.id.weeklyChart);

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
        Map<Integer, Integer> dayOfWeekCount = new HashMap<>();
        Map<String, Integer> dateCount = new HashMap<>();

        // Get current date for calculating daily averages
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(calendar.getTime());

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

                // Extract day of week from task date
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                dayOfWeekCount.put(dayOfWeek, dayOfWeekCount.getOrDefault(dayOfWeek, 0) + 1);

                // Count tasks per date
                String taskDate = dateFormat.format(calendar.getTime());
                dateCount.put(taskDate, dateCount.getOrDefault(taskDate, 0) + 1);
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

        // Find most productive day
        int mostProductiveDay = Calendar.SUNDAY;
        int maxDayCount = 0;
        for (Map.Entry<Integer, Integer> entry : dayOfWeekCount.entrySet()) {
            if (entry.getValue() > maxDayCount) {
                maxDayCount = entry.getValue();
                mostProductiveDay = entry.getKey();
            }
        }

        // Calculate average tasks per day
        int totalDays = Math.max(1, dateCount.size());
        float averageTasksPerDay = (float) totalTasks / totalDays;

        // Update UI
        tvTotalTasks.setText(String.format("Total Tasks: %d", totalTasks));
        tvCompletionRate.setText(String.format("Completion Rate: %.1f%%", completionRate));
        tvMostActiveCategory.setText(String.format("Most Active Category: %s", 
            mostActiveCategory.getDisplayName()));
        tvMostActiveTime.setText(String.format("Most Active Time: %02d:00", mostActiveHour));
        tvAverageTasksPerDay.setText(String.format("Average Tasks per Day: %.1f", averageTasksPerDay));
        tvMostProductiveDay.setText(String.format("Most Productive Day: %s", 
            getDayName(mostProductiveDay)));

        // Setup category statistics RecyclerView
        setupCategoryStats(categoryCount, totalTasks);

        // Setup weekly distribution chart
        setupWeeklyChart(dayOfWeekCount);
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return "Sunday";
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            default: return "Unknown";
        }
    }

    private void setupWeeklyChart(Map<Integer, Integer> dayOfWeekCount) {
        weeklyChart.removeAllViews();
        int maxCount = 0;
        for (Integer count : dayOfWeekCount.values()) {
            maxCount = Math.max(maxCount, count);
        }

        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            int count = dayOfWeekCount.getOrDefault(i, 0);
            
            LinearLayout barContainer = new LinearLayout(this);
            barContainer.setOrientation(LinearLayout.VERTICAL);
            barContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            barContainer.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            barContainer.setPadding(4, 0, 4, 0);

            // Create bar
            View bar = new View(this);
            float percentage = maxCount > 0 ? (float) count / maxCount : 0;
            int barHeight = (int) (160 * percentage);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                barHeight);
            bar.setLayoutParams(barParams);
            bar.setBackgroundColor(getResources().getColor(R.color.primary));

            // Create day label
            TextView dayLabel = new TextView(this);
            dayLabel.setText(getDayName(i).substring(0, 3));
            dayLabel.setGravity(Gravity.CENTER);
            dayLabel.setTextSize(12);

            barContainer.addView(bar);
            barContainer.addView(dayLabel);
            weeklyChart.addView(barContainer);
        }
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