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
import io.zak.delivery.adapters.CategoryListAdapter;
import io.zak.delivery.data.AppDatabase;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Brand;
import io.zak.delivery.data.entities.Category;
import io.zak.delivery.firebase.BrandEntry;
import io.zak.delivery.firebase.CategoryEntry;

public class CategoriesActivity extends AppCompatActivity {

    private static final String TAG = "Categories";

    private SearchView searchView;
    private ImageButton btnBack, btnSync;
    private RecyclerView recyclerView;
    private TextView tvNoCategories;
    private RelativeLayout progressGroup;

    private CategoryListAdapter adapter;

    private DatabaseReference mDatabase;
    private DatabaseReference mCategoriesRef;
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

    private List<Category> categoryList;
    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        btnBack = findViewById(R.id.btn_back);
        btnSync = findViewById(R.id.btn_sync);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoCategories = findViewById(R.id.tv_no_categories);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryListAdapter();
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

        if (mCategoriesRef == null) {
            mCategoriesRef = mDatabase.child("categories");
            mCategoriesRef.addValueEventListener(valueEventListener);
        }
    }

    private void replaceAll(DataSnapshot snapshot) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "deleting all categories from local database");
            return AppDatabaseImpl.getInstance(getApplicationContext()).categories().deleteAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rows -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "deleted " + rows + " rows");
            processSnapshot(snapshot);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
        }));
    }

    private void processSnapshot(DataSnapshot snapshot) {
        categoryList = new ArrayList<>();
        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
            CategoryEntry entry = postSnapshot.getValue(CategoryEntry.class);
            if (entry != null) {
                Category category = new Category();
                category.categoryId = entry.id;
                category.categoryName = entry.category;
                categoryList.add(category);
            }
        }
        addAll(categoryList);
        mCategoriesRef.removeEventListener(valueEventListener);
    }

    private void addAll(List<Category> categories) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            int count = 0;
            AppDatabase database = AppDatabaseImpl.getInstance(getApplicationContext());
            for (Category category : categories) {
                database.categories().insert(category);
                count++;
            }
            return count;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            Log.d(TAG, "added " + count + " categories");
            progressGroup.setVisibility(View.GONE);
            adapter.replaceAll(categories);
            tvNoCategories.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private void syncData() {

    }
}
