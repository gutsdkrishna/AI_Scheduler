package com.example.myapplication;

public enum Category {
    ALL("All Tasks", R.color.category_all),
    HEALTH("Health", R.color.category_health),
    WORK("Work", R.color.category_work),
    PERSONAL("Personal", R.color.category_personal),
    SHOPPING("Shopping", R.color.category_shopping),
    STUDY("Study", R.color.category_study),
    NORMAL("Normal", R.color.category_normal),
    OTHER("Other", R.color.category_other);

    private final String displayName;
    private final int colorResId;

    Category(String displayName, int colorResId) {
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