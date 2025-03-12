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
import io.zak.delivery.data.relations.OrderDetail;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView orNo, consumerName, total, date, status;
        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            orNo = view.findViewById(R.id.tv_orno);
            consumerName = view.findViewById(R.id.tv_consumer);
            total = view.findViewById(R.id.tv_amount);
            date = view.findViewById(R.id.tv_date);
            status = view.findViewById(R.id.tv_status);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> onItemClickListener.onItemClick(getAdapterPosition()));
        }
    }

    private final Comparator<OrderDetail> comparator = Comparator.comparing(orderDetail -> orderDetail.order.orNo);

    private final SortedList<OrderDetail> sortedList = new SortedList<>(OrderDetail.class, new SortedList.Callback<OrderDetail>() {
        @Override
        public int compare(OrderDetail o1, OrderDetail o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(OrderDetail oldItem, OrderDetail newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(OrderDetail item1, OrderDetail item2) {
            return item1.order.orderId == item2.order.orderId;
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

    public OrderListAdapter(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDetail orderDetail = sortedList.get(position);
        if (orderDetail != null) {
            holder.orNo.setText(orderDetail.order.orNo);
            holder.date.setText(Utils.humanizeDate(new Date(orderDetail.order.dateOrdered)));
            holder.consumerName.setText(orderDetail.consumer.consumerName);
            holder.total.setText(Utils.toStringMoneyFormat(orderDetail.order.totalAmount));
            holder.status.setText(orderDetail.order.orderStatus);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public OrderDetail getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<OrderDetail> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            OrderDetail detail = sortedList.get(i);
            if (!list.contains(detail)) sortedList.remove(detail);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
