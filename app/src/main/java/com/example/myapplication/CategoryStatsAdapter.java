package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryStatsAdapter extends RecyclerView.Adapter<CategoryStatsAdapter.ViewHolder> {
    private final List<StatisticsActivity.CategoryStat> categoryStats;

    public CategoryStatsAdapter(List<StatisticsActivity.CategoryStat> categoryStats) {
        this.categoryStats = categoryStats;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatisticsActivity.CategoryStat stat = categoryStats.get(position);
        
        holder.tvCategoryName.setText(stat.category.getDisplayName());
        holder.tvTaskCount.setText(String.format("%d tasks", stat.count));
        holder.tvPercentage.setText(String.format("%.1f%%", stat.percentage));
        holder.progressBar.setProgress((int) stat.percentage);
        
        // Set category color
        int color = ContextCompat.getColor(holder.itemView.getContext(), 
            stat.category.getColorResId());
        holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return categoryStats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvTaskCount;
        TextView tvPercentage;
        ProgressBar progressBar;

        ViewHolder(View view) {
            super(view);
            tvCategoryName = view.findViewById(R.id.tvCategoryName);
            tvTaskCount = view.findViewById(R.id.tvTaskCount);
            tvPercentage = view.findViewById(R.id.tvPercentage);
            progressBar = view.findViewById(R.id.progressBar);
        }
    }
} 