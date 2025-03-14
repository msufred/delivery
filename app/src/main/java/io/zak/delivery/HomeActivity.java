package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Brand;
import io.zak.delivery.data.entities.Category;
import io.zak.delivery.data.entities.Product;
import io.zak.delivery.data.entities.Supplier;
import io.zak.delivery.data.entities.User;
import io.zak.delivery.firebase.BrandEntry;
import io.zak.delivery.firebase.UserEntry;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";

    // widgets
    private ImageView profile;
    private TextView tvUsername, tvPosition, tvLicense;
    private ImageButton btnEdit;
    private CardView cardStocks, cardOrders, cardProducts, cardConsumers, cardBrands, cardCategories;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private User mUser;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private UserEntry mUserEntry;

    // data to sync
    private List<Brand> brandList;
    private List<Category> categoryList;
    private List<Product> productList;
    private List<Supplier> supplierList;

    // refs
    private DatabaseReference brandRef;

    // value listeners
    private final ValueEventListener brandEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            replaceBrands(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled brands sync", error.toException());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getWidgets() {
        profile = findViewById(R.id.profile);
        tvUsername = findViewById(R.id.tv_username);
        tvPosition = findViewById(R.id.tv_position);
        tvLicense = findViewById(R.id.tv_license);
        btnEdit = findViewById(R.id.btn_edit);
        cardStocks = findViewById(R.id.card_stocks);
        cardOrders = findViewById(R.id.card_orders);
        cardProducts = findViewById(R.id.card_products);
        cardConsumers = findViewById(R.id.card_consumers);
        cardBrands = findViewById(R.id.card_brands);
        cardCategories = findViewById(R.id.card_categories);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnEdit.setOnClickListener(v -> {
            // TODO
        });

        cardStocks.setOnClickListener(v -> {
            startActivity(new Intent(this, StocksActivity.class));
        });

         cardOrders.setOnClickListener(v -> {
             startActivity(new Intent(this, OrdersActivity.class));
         });

         cardProducts.setOnClickListener(v -> {
             startActivity(new Intent(this, ProductsActivity.class));
         });

         cardConsumers.setOnClickListener(v -> {
             startActivity(new Intent(this, ConsumersActivity.class));
         });

         cardBrands.setOnClickListener(v -> {
             startActivity(new Intent(this, BrandsActivity.class));
         });

         cardCategories.setOnClickListener(v -> {
             startActivity(new Intent(this, CategoriesActivity.class));
         });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "getting user info from firebase");
        progressGroup.setVisibility(View.VISIBLE);
        mDatabase.child("users").child(user.getUid())
                .get()
                .addOnCompleteListener(this, task -> {
                    progressGroup.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "success! displaying info");
                        mUserEntry = task.getResult().getValue(UserEntry.class);
                        displayInfo(mUserEntry);
                        // syncData();
                    } else {
                        Toast.makeText(this, "Failed to fetch User info.", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "failed to fetch user info", task.getException());
                    }
                });
    }

    private void displayInfo(UserEntry entry) {
        if (entry != null) {
            tvUsername.setText(entry.fullName);
            if (entry.position != null) tvPosition.setText(entry.position);
            if (entry.license != null) tvLicense.setText(entry.license);
        }
    }

    private void syncData() {
        Log.d(TAG, "syncing data");
        progressGroup.setVisibility(View.VISIBLE);
    }

    private void replaceBrands(DataSnapshot snapshot) {
        Log.d(TAG, "fetching brands...");
        brandList = new ArrayList<>();
        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
            BrandEntry entry = postSnapshot.getValue(BrandEntry.class);
            if (entry != null) {
                Brand brand = new Brand();
                brand.brandId = entry.id;
                brand.brandName = entry.brand;
                brandList.add(brand);
            }
        }
        brandRef.removeEventListener(brandEventListener);
        Log.d(TAG, "done fetching brands!");
    }
}
