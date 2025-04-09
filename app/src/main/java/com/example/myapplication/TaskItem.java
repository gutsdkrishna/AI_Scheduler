package com.example.myapplication;

import java.util.Date;

public class TaskItem {
    private String task;
    private String time;
    private Category category;
    private boolean completed;
    private Date createdAt;
    private Date completedAt;
    private RecurrenceType recurrenceType;
    private int recurrenceId; // Used to identify recurring reminders
    private Priority priority; // New field for task priority

    public enum RecurrenceType {
        NONE,
        DAILY,
        WEEKLY,
        MONTHLY
    }

    public enum Priority {
        LOW("Low", R.color.priority_low),
        MEDIUM("Medium", R.color.priority_medium),
        HIGH("High", R.color.priority_high);

        private final String displayName;
        private final int colorResId;

        Priority(String displayName, int colorResId) {
            this.displayName = displayName;
            this.colorResId = colorResId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColorResId() {
            return colorResId;
        }
    }

    public TaskItem(String task, String time) {
        this(task, time, Category.OTHER);
    }

    public TaskItem(String task, String time, Category category) {
        this(task, time, category, RecurrenceType.NONE);
    }

    public TaskItem(String task, String time, Category category, RecurrenceType recurrenceType) {
        this(task, time, category, recurrenceType, Priority.MEDIUM);
    }

    public TaskItem(String task, String time, Category category, RecurrenceType recurrenceType, Priority priority) {
        this.task = task;
        this.time = time;
        this.category = category;
        this.completed = false;
        this.createdAt = new Date();
        this.recurrenceType = recurrenceType;
        this.recurrenceId = 0;
        this.priority = priority;
    }

    public String getTask() {
        return task;
    }

    public String getTime() {
        return time;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.completedAt = new Date();
        } else {
            this.completedAt = null;
        }
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(RecurrenceType recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public int getRecurrenceId() {
        return recurrenceId;
    }

    public void setRecurrenceId(int recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
} 