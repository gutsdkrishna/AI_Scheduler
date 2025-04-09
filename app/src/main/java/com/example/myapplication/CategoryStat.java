package com.example.myapplication;

public class CategoryStat {
    private Category category;
    private int count;

    public CategoryStat(Category category, int count) {
        this.category = category;
        this.count = count;
    }

    public Category getCategory() {
        return category;
    }

    public int getCount() {
        return count;
    }
} 