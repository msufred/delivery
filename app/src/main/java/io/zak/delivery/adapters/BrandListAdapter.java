package io.zak.delivery.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.Comparator;
import java.util.List;

import io.zak.delivery.R;
import io.zak.delivery.data.entities.Brand;

public class BrandListAdapter extends RecyclerView.Adapter<BrandListAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tv_name);
        }
    }

    private final Comparator<Brand> comparator = Comparator.comparing(brand -> brand.brandName);

    private final SortedList<Brand> sortedList = new SortedList<>(Brand.class, new SortedList.Callback<>() {
        @Override
        public int compare(Brand o1, Brand o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Brand oldItem, Brand newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Brand item1, Brand item2) {
            return item1.brandId == item2.brandId;
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


    public BrandListAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Brand brand = sortedList.get(position);
        if (brand != null) {
            holder.name.setText(brand.brandName);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void addItem(Brand brand) {
        sortedList.add(brand);
    }

    public void replaceAll(List<Brand> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Brand brand = sortedList.get(i);
            if (!list.contains(brand)) sortedList.remove(brand);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

}

