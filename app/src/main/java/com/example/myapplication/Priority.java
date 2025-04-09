package com.example.myapplication;

public enum Priority {
    HIGH(3, "High", R.color.priority_high),
    MEDIUM(2, "Medium", R.color.priority_medium),
    LOW(1, "Low", R.color.priority_low);

    private final int value;
    private final String displayName;
    private final int colorResId;

    Priority(int value, String displayName, int colorResId) {
        this.value = value;
        this.displayName = displayName;
        this.colorResId = colorResId;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColorResId() {
        return colorResId;
    }

    public static Priority fromValue(int value) {
        for (Priority priority : Priority.values()) {
            if (priority.value == value) {
                return priority;
            }
        }
        return MEDIUM; // Default to medium if value not found
    }
} 