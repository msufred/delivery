package io.zak.delivery;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import io.zak.delivery.adapters.BrandListAdapter;
import io.zak.delivery.data.AppDatabase;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Brand;
import io.zak.delivery.firebase.BrandEntry;

public class BrandsActivity extends AppCompatActivity {

    private static final String TAG = "Brands";

    private SearchView searchView;
    private ImageButton btnBack, btnSync;
    private RecyclerView recyclerView;
    private TextView tvNoBrands;
    private RelativeLayout progressGroup;

    private BrandListAdapter adapter;

    private DatabaseReference mDatabase;
    private DatabaseReference mBrandsRef;
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
    private List<Brand> brandList;

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brands);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        btnBack = findViewById(R.id.btn_back);
        btnSync = findViewById(R.id.btn_sync);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoBrands = findViewById(R.id.tv_no_brands);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BrandListAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });
        btnSync.setOnClickListener(v -> syncData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mBrandsRef == null) {
            mBrandsRef = mDatabase.child("brands");
            mBrandsRef.addValueEventListener(valueEventListener);
        }
    }

    private void replaceAll(DataSnapshot snapshot) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "deleting all brands from local database");
            return AppDatabaseImpl.getInstance(getApplicationContext()).brands().deleteAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rows -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "deleted " + rows + " rows");
            processSnapshot(snapshot);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
        }));
    }

    private void processSnapshot(DataSnapshot snapshot) {
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
        addAll(brandList);
        mBrandsRef.removeEventListener(valueEventListener);
    }

    private void addAll(List<Brand> brands) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            int count = 0;
            AppDatabase database = AppDatabaseImpl.getInstance(getApplicationContext());
            for (Brand brand : brands) {
                database.brands().insert(brand);
                count++;
            }
            return count;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            Log.d(TAG, "added " + count + " brands");
            progressGroup.setVisibility(View.GONE);
            adapter.replaceAll(brands);
            tvNoBrands.setVisibility(brands.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private void syncData() {

    }
}
