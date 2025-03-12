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
import io.zak.delivery.adapters.StockListAdapter;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.VehicleStock;
import io.zak.delivery.data.relations.VehicleStockDetail;

public class StocksActivity extends AppCompatActivity implements StockListAdapter.OnItemClickListener {

    private static final String TAG = "Stocks";

    // widgets
    private ImageButton btnBack;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoStocks;
    private Button btnAdd, btnScan;
    private RelativeLayout progressGroup;

    private StockListAdapter adapter;
    private List<VehicleStockDetail> vehicleStockList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stocks);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoStocks = findViewById(R.id.tv_no_stocks);
        btnAdd = findViewById(R.id.btn_add);
        btnScan = findViewById(R.id.btn_scan);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StockListAdapter(this);
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
        btnScan.setOnClickListener(v -> {
            // TODO
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // get vehicle id
        int vehicleId = getIntent().getIntExtra("vehicle_id", -1);
        if (vehicleId == -1) {
            dialogBuilder.setTitle("Invalid")
                    .setMessage("Unknown vehicle id.")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Retrieving vehicle stock entries");
            return AppDatabaseImpl.getInstance(getApplicationContext()).vehicleStocks().getVehicleStocks(vehicleId);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size());
            progressGroup.setVisibility(View.GONE);
            adapter.replaceAll(list);
            vehicleStockList = list;
            tvNoStocks.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            progressGroup.setVisibility(View.GONE);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while retrieving vehicle stock entries: " + err)
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
        List<VehicleStockDetail> filteredList = filter(vehicleStockList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<VehicleStockDetail> filter(List<VehicleStockDetail> stockList, String query) {
        List<VehicleStockDetail> list = new ArrayList<>();
        String str = query.toLowerCase();
        for (VehicleStockDetail stockDetail : stockList) {
            if (stockDetail.product.productName.toLowerCase().contains(str)) {
                list.add(stockDetail);
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
