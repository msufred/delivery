package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import io.zak.delivery.firebase.BrandEntry;
import io.zak.delivery.firebase.CategoryEntry;
import io.zak.delivery.firebase.ProductEntry;
import io.zak.delivery.firebase.SupplierEntry;

public class ViewProductActivity extends AppCompatActivity {

    private static final String TAG = "ViewProduct";

    // Widgets
    private ImageButton btnClose, btnEdit, btnSelectProfile;
    private ImageView qrCodeView;
    private TextView tvName, tvSupplier, tvBrand, tvCategory, tvPrice, tvDescription, tvCritLevel;
    private ImageView profile;
    private RelativeLayout progressGroup;

    private AlertDialog.Builder dialogBuilder;

    private DatabaseReference mDatabase;
    private ProductEntry productEntry;
    private BrandEntry brandEntry;
    private CategoryEntry categoryEntry;
    private SupplierEntry supplierEntry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_product);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnClose = findViewById(R.id.btn_close);
        btnSelectProfile = findViewById(R.id.btn_select_profile);
        qrCodeView = findViewById(R.id.iv_qrcode);
        tvName = findViewById(R.id.tv_name);
        tvSupplier = findViewById(R.id.tv_supplier);
        tvBrand = findViewById(R.id.tv_brand);
        tvCategory = findViewById(R.id.tv_category);
        tvPrice = findViewById(R.id.tv_price);
        tvDescription = findViewById(R.id.tv_description);
        profile = findViewById(R.id.profile);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnClose.setOnClickListener(v -> goBack());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        int id = getIntent().getIntExtra("product_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Product ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        fetchProductDetails(id);
    }

    private void fetchProductDetails(int productId) {
        mDatabase.child("products").child(String.valueOf(productId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        productEntry = snapshot.getValue(ProductEntry.class);
                        if (productEntry != null) {
                            fetchBrandDetails(productEntry.brandId);
                        } else {
                            progressGroup.setVisibility(View.GONE);
                            showErrorDialog("Product not found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressGroup.setVisibility(View.GONE);
                        Log.e(TAG, "Database error: " + error.getMessage());
                        showErrorDialog("Error fetching product: " + error.getMessage());
                    }
                });
    }

    private void fetchBrandDetails(int brandId) {
        mDatabase.child("brands").child(String.valueOf(brandId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        brandEntry = snapshot.getValue(BrandEntry.class);
                        fetchCategoryDetails(productEntry.categoryId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressGroup.setVisibility(View.GONE);
                        Log.e(TAG, "Database error: " + error.getMessage());
                        showErrorDialog("Error fetching brand: " + error.getMessage());
                    }
                });
    }

    private void fetchCategoryDetails(int categoryId) {
        mDatabase.child("categories").child(String.valueOf(categoryId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        categoryEntry = snapshot.getValue(CategoryEntry.class);
                        fetchSupplierDetails(productEntry.supplierId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressGroup.setVisibility(View.GONE);
                        Log.e(TAG, "Database error: " + error.getMessage());
                        showErrorDialog("Error fetching category: " + error.getMessage());
                    }
                });
    }

    private void fetchSupplierDetails(int supplierId) {
        mDatabase.child("suppliers").child(String.valueOf(supplierId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        supplierEntry = snapshot.getValue(SupplierEntry.class);
                        progressGroup.setVisibility(View.GONE);
                        displayInfo();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressGroup.setVisibility(View.GONE);
                        Log.e(TAG, "Database error: " + error.getMessage());
                        showErrorDialog("Error fetching supplier: " + error.getMessage());
                    }
                });
    }

    private void displayInfo() {
        if (productEntry != null) {
            tvName.setText(productEntry.name);

            if (brandEntry != null) {
                tvBrand.setText(brandEntry.brand);
            } else {
                tvBrand.setText("Unknown Brand");
            }

            if (categoryEntry != null) {
                tvCategory.setText(categoryEntry.category);
            } else {
                tvCategory.setText("Unknown Category");
            }

            tvPrice.setText(Utils.toStringMoneyFormat(productEntry.price));

            if (supplierEntry != null) {
                tvSupplier.setText(supplierEntry.name);
            } else {
                tvSupplier.setText("Unknown Supplier");
            }

            if (productEntry.description != null && !productEntry.description.isEmpty()) {
                tvDescription.setText(productEntry.description);
            }

            if (tvCritLevel != null) {
                tvCritLevel.setText(String.valueOf(productEntry.criticalLevel));
            }

            // QR Code
            String str = String.format(Locale.getDefault(),
                    "id=%d;" +
                            "name=%s;" +
                            "brand=%d;" +
                            "category=%d;" +
                            "supplier=%d;" +
                            "crit=%d;" +
                            "price=%.2f;" +
                            "desc=%s",
                    productEntry.id,
                    productEntry.name,
                    productEntry.brandId,
                    productEntry.categoryId,
                    productEntry.supplierId,
                    productEntry.criticalLevel,
                    productEntry.price,
                    productEntry.description);

            qrCodeView.setImageBitmap(Utils.generateQrCode(str, 200, 200));
        }
    }

    private void showErrorDialog(String message) {
        dialogBuilder.setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    goBack();
                });
        dialogBuilder.create().show();
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
    }
}