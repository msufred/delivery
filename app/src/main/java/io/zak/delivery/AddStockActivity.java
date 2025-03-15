package io.zak.delivery;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.VehicleStock;

public class AddStockActivity extends AppCompatActivity {

    private static final String TAG = "AddStock";

    // widgets
    private EditText etWarehouseId, etProductId, etProductName, etSellingPrice, etQuantity;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    // temp variables
    private int vehicleId, warehouseId, productId, quantity;
    private String productName;
    private double sellingPrice;
    private double subTotal = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_stock);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etWarehouseId = findViewById(R.id.et_warehouse_id);
        etProductId = findViewById(R.id.et_product_id);
        etProductName = findViewById(R.id.et_name);
        etSellingPrice = findViewById(R.id.et_price);
        etQuantity = findViewById(R.id.et_quantity);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // get intent extras
        boolean fromScan = getIntent().getBooleanExtra("from_scan", false);
        if (fromScan) {
            vehicleId = getIntent().getIntExtra("vehicle_id", -1);
            warehouseId = getIntent().getIntExtra("stock_id", -1);
            productId = getIntent().getIntExtra("product_id", -1);
            productName = getIntent().getStringExtra("name");
            sellingPrice = getIntent().getDoubleExtra("price", 0);
            quantity = getIntent().getIntExtra("qty", 0);
            subTotal = sellingPrice * quantity;

            // display
            etWarehouseId.setText(String.valueOf(warehouseId));
            etProductId.setText(String.valueOf(productId));
            etProductName.setText(productName);
            etSellingPrice.setText(String.format(Locale.getDefault(), "%.2f", sellingPrice));
            etQuantity.setText(String.valueOf(quantity));
        }
    }

    private boolean validated() {
        boolean isValid = true;
        if (etWarehouseId.getText().toString().isBlank()) {
            etWarehouseId.setError("Required");
            isValid = false;
        }
        if (etProductId.getText().toString().isBlank()) {
            etProductId.setError("Required");
            isValid = false;
        }
        if (etProductName.getText().toString().isBlank()) {
            etProductName.setError("Required");
            isValid = false;
        }
        if (etSellingPrice.getText().toString().isBlank()) {
            etSellingPrice.setError("Required");
            isValid = false;
        }
        if (etQuantity.getText().toString().isBlank()) {
            etQuantity.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void saveAndClose() {
        warehouseId = Integer.parseInt(etWarehouseId.getText().toString().trim());
        productId = Integer.parseInt(etProductId.getText().toString().trim());
        productName = Utils.normalize(etProductName.getText().toString());
        sellingPrice = Double.parseDouble(etSellingPrice.getText().toString().trim());
        quantity = Integer.parseInt(etQuantity.getText().toString().trim());
        subTotal = quantity * sellingPrice;

        VehicleStock stock = new VehicleStock();
        stock.fkVehicleId = vehicleId;
        stock.fkWarehouseStockId = warehouseId;
        stock.fkProductId = productId;
        stock.quantity = quantity;
        stock.orderedQuantity = 0;
        stock.sellingPrice = sellingPrice;
        stock.totalAmount = subTotal;
        stock.dateOrdered = new Date().getTime();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving stock entry.");
            return AppDatabaseImpl.getInstance(getApplicationContext()).vehicleStocks().insert(stock);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Returned with id=" + id.intValue());
            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
            goBack();
        }));
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
