package io.zak.delivery;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
import io.zak.delivery.firebase.AssignedVehicleEntry;

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

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;
    private AssignedVehicleEntry vehicleEntry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
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
            Intent intent = new Intent(this, AddOrderActivity.class);
            intent.putExtra("user_id", vehicleEntry.userId);
            intent.putExtra("vehicle_id", vehicleEntry.vehicleId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check user
        mUser = mAuth.getCurrentUser();
        if (mUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // get assigned vehicle
        progressGroup.setVisibility(View.VISIBLE);
        mDatabase.child("assigned_vehicles").child(mUser.getUid()).get()
                .addOnCompleteListener(this, task -> {
                    progressGroup.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        vehicleEntry = task.getResult().getValue(AssignedVehicleEntry.class);
                        if (vehicleEntry == null) {
                            dialogBuilder.setTitle("Invalid")
                                    .setMessage("No assigned vehicle!")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        dialog.dismiss();
                                        goBack();
                                    });
                            dialogBuilder.create().show();
                        } else {
                            fetchOrders();
                        }
                    } else {
                        Log.w(TAG, "failed to get assigned vehicle", task.getException());
                        dialogBuilder.create().show();
                        dialogBuilder.setTitle("Error")
                                .setMessage(task.getException().toString())
                                .setPositiveButton("OK", (dialog, which) -> {
                                    dialog.dismiss();
                                    goBack();
                                });
                        goBack();
                    }
                });
    }

    private void fetchOrders() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "fetch order entries");
            return AppDatabaseImpl.getInstance(getApplicationContext()).orders().getOrdersWithDetail();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "returned with list size=" + list.size());
            progressGroup.setVisibility(View.GONE);
            adapter.replaceAll(list);
            orderDetailList = list;
            tvNoOrders.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
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
        return oList;
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
