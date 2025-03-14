package io.zak.delivery;

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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.adapters.ProductListAdapter;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Product;

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
    private FirebaseUser mUser;

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
        mUser = mAuth.getCurrentUser();
        if (mUser != null) {
            // TODO update UI, show user is signed-in
        } else {
            // TODO update UI, show user is NOT signed-in
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching product entries");
            return AppDatabaseImpl.getInstance(getApplicationContext()).products().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + list.size());
            adapter.replaceAll(list);
            productList = list;
            tvNoProducts.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching product entries: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void syncData() {
        Firebase firebase = Firebase.INSTANCE;
    }

    @Override
    public void onItemClick(int position) {
        // TODO view product
    }

    // TODO
    private void register(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Registration Success");
                        mUser = mAuth.getCurrentUser();
                        // TODO display info
                    } else {
                        Log.w(TAG, "Registration Failure", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // TODO
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Sign In Success");
                        mUser = mAuth.getCurrentUser();
                        // TODO display info
                    } else {
                        Log.w(TAG, "Sign In Failure", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
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
