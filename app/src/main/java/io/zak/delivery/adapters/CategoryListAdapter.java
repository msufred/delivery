package io.zak.delivery.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.zak.delivery.R;
import io.zak.delivery.data.entities.Category;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tv_name);
        }
    }

    private final Comparator<Category> comparator = Comparator.comparing(category -> category.categoryName);

    private final SortedList<Category> sortedList = new SortedList<>(Category.class, new SortedList.Callback<>() {
        @Override
        public int compare(Category o1, Category o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Category oldItem, Category newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Category item1, Category item2) {
            return item1.categoryId == item2.categoryId;
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    });

    public CategoryListAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = sortedList.get(position);
        if (category != null) {
            holder.name.setText(category.categoryName);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }


    public void addItem(Category category) {
        sortedList.add(category);
    }

    public void replaceAll(List<Category> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Category category = sortedList.get(i);
            if (!list.contains(category)) sortedList.remove(category);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}

