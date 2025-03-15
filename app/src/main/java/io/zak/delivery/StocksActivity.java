package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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
import io.zak.delivery.firebase.AssignedVehicleEntry;
import io.zak.delivery.firebase.VehicleEntry;

public class StocksActivity extends AppCompatActivity implements StockListAdapter.OnItemClickListener {

    private static final String TAG = "Stocks";

    // widgets
    private TextView title;
    private ImageButton btnBack;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoStocks;
    private Button btnAdd, btnScan;
    private LinearLayout actionGroup;
    private RelativeLayout progressGroup;

    private StockListAdapter adapter;
    private List<VehicleStockDetail> vehicleStockList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    // for QR Code scanning
    private ActivityResultLauncher<ScanOptions> qrCodeLauncher;
    private ScanOptions scanOptions;

    // scan result variables
    public int deliveryOrderId;
    public int warehouseStockId;    // warehouse the stock's belongs/stored
    public int productId;           // product details
    public String productName;      //
    public double price;            //
    public int quantity;            // quantity from delivery order item
    public double subtotal;         // subtotal from delivery order item

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private AssignedVehicleEntry mAssignedVehicle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stocks);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // register qrCodeLauncher
        qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                processScanResult(result.getContents());
            } else {
                Toast.makeText(this, "QR Code Scan Cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getWidgets() {
        title = findViewById(R.id.title);
        btnBack = findViewById(R.id.btn_back);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoStocks = findViewById(R.id.tv_no_stocks);
        btnAdd = findViewById(R.id.btn_add);
        btnScan = findViewById(R.id.btn_scan);
        actionGroup = findViewById(R.id.action_group);
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
            startActivity(new Intent(this, AddStockActivity.class));
        });

        btnScan.setOnClickListener(v -> qrCodeLauncher.launch(getScanOptions()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check signed in user
        Log.d(TAG, "checking signed-in user");
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // check if there is an assigned vehicle for the current user
        Log.d(TAG, "checking assigned vehicle");
        progressGroup.setVisibility(View.VISIBLE);
        mDatabase.child("assigned_vehicles").child(user.getUid()).get()
                .addOnCompleteListener(this, task -> {
                    progressGroup.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "success");
                        mAssignedVehicle = task.getResult().getValue(AssignedVehicleEntry.class);
                        if (mAssignedVehicle != null) {
                            displayInfo(mAssignedVehicle);
                            // fetch items
                            fetchStocks(mAssignedVehicle.vehicleId);
                        } else {
                            actionGroup.setVisibility(View.GONE); // hide action buttons
                            dialogBuilder.setMessage("No vehicle assigned.")
                                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                            dialogBuilder.create().show();
                        }
                    }
                });
    }

    private void displayInfo(AssignedVehicleEntry assignedVehicleEntry) {
        title.setText(String.format("%s (%s)", assignedVehicleEntry.vehicleName, assignedVehicleEntry.plateNo));
    }

    private void fetchStocks(int vehicleId) {

    }

    private ScanOptions getScanOptions() {
        if (scanOptions == null) scanOptions = new ScanOptions();
        scanOptions.setCaptureActivity(PortraitCaptureActivity.class);
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setCameraId(0);
        scanOptions.setPrompt("Scan QR Code");
        return scanOptions;
    }

    private void processScanResult(String qrCodeResult) {
        resetScanResultValues();

        // Break down text (use ; as delimiter)
        String[] strArr = qrCodeResult.split(";");
        for (String str : strArr) {
            String[] arr = str.split("=");
            String key = arr[0];
            String value = arr[1];

            switch (key) {
                case "order_id": deliveryOrderId = Integer.parseInt(value.trim()); break;
                case "stock_id": warehouseStockId = Integer.parseInt(value.trim()); break;
                case "product_id": productId = Integer.parseInt(value.trim()); break;
                case "name": productName = value; break;
                case "price": price = Double.parseDouble(value.trim()); break;
                case "qty": quantity = Integer.parseInt(value.trim()); break;
                case "subtotal": subtotal = Double.parseDouble(value.trim()); break;
                default:
            }
        }

        // show AddStockActivity
        startActivity(processIntent());
    }

    @NonNull
    private Intent processIntent() {
        Intent intent = new Intent(this, AddStockActivity.class);
        intent.putExtra("from_scan", true);
        intent.putExtra("vehicle_id", mAssignedVehicle.vehicleId);
        intent.putExtra("order_id", deliveryOrderId);
        intent.putExtra("stock_id", warehouseStockId);
        intent.putExtra("product_id", productId);
        intent.putExtra("name", productName);
        intent.putExtra("price", price);
        intent.putExtra("qty", quantity);
        intent.putExtra("subtotal", subtotal);
        return intent;
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

    private void resetScanResultValues() {
        deliveryOrderId = -1;
        warehouseStockId = -1;
        productId = -1;
        productName = null;
        price = 0;
        quantity = 0;
        subtotal = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources");
        disposables.dispose();
    }
}
