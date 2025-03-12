package io.zak.delivery;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.adapters.OrderListAdapter;
import io.zak.delivery.data.AppDatabase;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.User;
import io.zak.delivery.data.relations.OrderDetail;

public class OrdersActivity extends AppCompatActivity implements OrderListAdapter.OnItemClickListener {

    private static final String TAG = "Orders";

    private ImageButton btnBack;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoOrders;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    private OrderListAdapter adapter;
    private List<OrderDetail> orderDetailList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private User mUser; // current user/employee/driver

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoOrders = findViewById(R.id.tv_no_orders);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderListAdapter(this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearch(newText);
                return false;
            }
        });
        btnAdd.setOnClickListener(v -> {
            // TODO
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        AppDatabase database = AppDatabaseImpl.getInstance(getApplicationContext());
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Retrieving user info");
            return database.users().getUserById(Utils.getLoginId(this));
        }).flatMap(users -> {
            Log.d(TAG, "Returned with users=" + users.size());
            if (!users.isEmpty()) mUser = users.get(0);
            // return all orders
            return Single.fromCallable(() -> database.orders().getOrdersWithDetail());
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            Log.d(TAG, "Returned with orders=" + orders.size());
            orderDetailList = orders;
            adapter.replaceAll(orders);
            tvNoOrders.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while retrieving user info and list of orders: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        // TODO
    }

    private void onSearch(String query) {
        List<OrderDetail> filteredList = filter(orderDetailList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<OrderDetail> filter(List<OrderDetail> list, String query) {
        List<OrderDetail> oList = new ArrayList<>();
        String str = query.toLowerCase();
        for (OrderDetail detail : list) {
            if (detail.order.orNo.toLowerCase().contains(str) || detail.consumer.consumerName.toLowerCase().contains(str)) {
                oList.add(detail);
            }
        }
        return list;
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources");
        disposables.dispose();
    }
}
