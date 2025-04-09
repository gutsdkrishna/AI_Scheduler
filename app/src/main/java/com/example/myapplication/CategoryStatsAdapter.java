package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CategoryStatsAdapter extends RecyclerView.Adapter<CategoryStatsAdapter.ViewHolder> {
    private final List<CategoryStat> categoryStats;

    public CategoryStatsAdapter(List<CategoryStat> categoryStats) {
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
        CategoryStat stat = categoryStats.get(position);
        holder.bind(stat);
    }

    @Override
    public int getItemCount() {
        return categoryStats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView textCategory;
        private final TextView textCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            textCategory = itemView.findViewById(R.id.textCategory);
            textCount = itemView.findViewById(R.id.textCount);
        }

        public void bind(CategoryStat stat) {
            textCategory.setText(stat.getCategory().getDisplayName());
            textCount.setText(stat.getCount() + " tasks");
            
            // Set card stroke color based on category
            int color = ContextCompat.getColor(itemView.getContext(), stat.getCategory().getColorResId());
            cardView.setStrokeColor(color);
            cardView.setStrokeWidth(2);
        }
    }
} 