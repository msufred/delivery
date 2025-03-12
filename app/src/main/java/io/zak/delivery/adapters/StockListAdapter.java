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
import java.util.Date;
import java.util.List;

import io.zak.delivery.R;
import io.zak.delivery.Utils;
import io.zak.delivery.data.relations.VehicleStockDetail;

public class StockListAdapter extends RecyclerView.Adapter<StockListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, stock, date;
        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            stock = view.findViewById(R.id.tv_quantity);
            date = view.findViewById(R.id.tv_date);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> onItemClickListener.onItemClick(getAdapterPosition()));
        }
    }

    private final Comparator<VehicleStockDetail> comparator = Comparator.comparing(stock -> stock.product.productName);

    private final SortedList<VehicleStockDetail> sortedList = new SortedList<>(VehicleStockDetail.class, new SortedList.Callback<>() {
        @Override
        public int compare(VehicleStockDetail o1, VehicleStockDetail o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(VehicleStockDetail oldItem, VehicleStockDetail newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(VehicleStockDetail item1, VehicleStockDetail item2) {
            return item1.vehicleStock.vehicleStockId == item2.vehicleStock.vehicleStockId;
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

    private final OnItemClickListener onItemClickListener;

    public StockListAdapter(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_stock, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VehicleStockDetail stockDetail = sortedList.get(position);
        if (stockDetail != null) {
            holder.name.setText(stockDetail.product.productName);
            holder.stock.setText(String.valueOf(stockDetail.vehicleStock.quantity));
            holder.date.setText(Utils.humanizeDate(new Date(stockDetail.vehicleStock.dateOrdered)));
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public VehicleStockDetail getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<VehicleStockDetail> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            VehicleStockDetail stockDetail = sortedList.get(i);
            if (!list.contains(stockDetail)) sortedList.remove(stockDetail);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
