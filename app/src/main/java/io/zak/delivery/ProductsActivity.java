package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
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
import io.zak.delivery.adapters.ProductListAdapter;
import io.zak.delivery.data.AppDatabase;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Product;
import io.zak.delivery.firebase.ProductEntry;

public class ProductsActivity extends AppCompatActivity implements ProductListAdapter.OnItemClickListener {

    private static final String TAG = "Products";

    // widgets
    private ImageButton btnBack, btnRefresh;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoProducts;
    private Button btnAdd, btnScan;
    private RelativeLayout progressGroup;

    // for RecyclerView
    private ProductListAdapter adapter;
    private List<Product> productList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference mProductsRef;
    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            replaceAll(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);
        getWidgets();
        setListeners();

        mAuth = FirebaseAuth.getInstance(); // initialize FirebaseAuth
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        btnRefresh = findViewById(R.id.btn_refresh);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoProducts = findViewById(R.id.tv_no_products);
        btnAdd = findViewById(R.id.btn_add);
        btnScan = findViewById(R.id.btn_scan);
        progressGroup = findViewById(R.id.progress_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductListAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnRefresh.setOnClickListener(v -> syncData());
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
            // TODO show add product
        });
        btnScan.setOnClickListener(v -> {
            // TODO show scan product
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // check if user is signed-in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

//        progressGroup.setVisibility(View.VISIBLE);
//        disposables.add(Single.fromCallable(() -> {
//            Log.d(TAG, "Fetching product entries");
//            return AppDatabaseImpl.getInstance(getApplicationContext()).products().getAll();
//        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
//            progressGroup.setVisibility(View.GONE);
//            Log.d(TAG, "Returned with list size=" + list.size());
//            adapter.replaceAll(list);
//            productList = list;
//            tvNoProducts.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
//        }, err -> {
//            progressGroup.setVisibility(View.GONE);
//            Log.e(TAG, "Database error: " + err);
//            dialogBuilder.setTitle("Database Error")
//                    .setMessage("Error while fetching product entries: " + err)
//                    .setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
//            dialogBuilder.create().show();
//        }));

        if (mProductsRef == null) {
            mProductsRef = mDatabase.child("products");
            mProductsRef.addValueEventListener(valueEventListener);
        }
    }

    private void replaceAll(DataSnapshot snapshot) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting all products.");
            return AppDatabaseImpl.getInstance(getApplicationContext()).products().deleteAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rows -> {
            Log.d(TAG, "deleted " + rows + " rows");
            progressGroup.setVisibility(View.GONE);
            processSnapshot(snapshot);
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private void processSnapshot(DataSnapshot snapshot) {
        List<Product> list = new ArrayList<>();
        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
            ProductEntry entry = postSnapshot.getValue(ProductEntry.class);
            if (entry != null) {
                Product product = new Product();
                product.productId = entry.id;
                product.fkBrandId = entry.brandId;
                product.fkCategoryId = entry.categoryId;
                product.fkSupplierId = entry.supplierId;
                product.productName = entry.name;
                product.price = entry.price;
                product.criticalLevel = entry.criticalLevel;
                product.productDescription = entry.description;
                list.add(product);
            }
        }
        addAll(list);
        mProductsRef.removeEventListener(valueEventListener);
    }

    private void addAll(List<Product> products) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
           int count = 0;
            AppDatabase database = AppDatabaseImpl.getInstance(getApplicationContext());
            for (Product product : products) {
                database.products().insert(product);
                count++;
            }
            return count;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            Log.d(TAG, "added " + count + " products");
            progressGroup.setVisibility(View.GONE);
            adapter.replaceAll(products);
            productList = products;
            tvNoProducts.setVisibility(products.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private void syncData() {
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Product product = adapter.getItem(position);
            if (product != null) {
                Log.d(TAG, "Product selected: " + product.productName);
                // TODO
                Intent intent = new Intent(this, ViewProductActivity.class);
                intent.putExtra("product_id", product.productId);
                startActivity(intent);
            }
        }
    }

    private void onSearch(String query) {
        List<Product> filteredList = filter(productList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Product> filter(List<Product> list, String query) {
        List<Product> pList = new ArrayList<>();
        String str = query.toLowerCase();
        for (Product p : list) {
            if (p.productName.toLowerCase().contains(str)) pList.add(p);
        }
        return pList;
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
