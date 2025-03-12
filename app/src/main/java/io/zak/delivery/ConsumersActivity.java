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
import io.zak.delivery.adapters.ConsumerListAdapter;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Consumer;

public class ConsumersActivity extends AppCompatActivity implements ConsumerListAdapter.OnItemClickListener {

    private static final String TAG = "Consumer";

    // widgets
    private ImageButton btnBack;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoConsumers;
    private Button btnAdd;
    private RelativeLayout progressLayout;

    private ConsumerListAdapter adapter;
    private List<Consumer> consumerList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumers);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoConsumers = findViewById(R.id.tv_no_consumers);
        btnAdd = findViewById(R.id.btn_add);
        progressLayout = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConsumerListAdapter(this);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressLayout.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Retrieving consumer entries");
            return AppDatabaseImpl.getInstance(getApplicationContext()).consumers().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size());
            progressLayout.setVisibility(View.GONE);
            consumerList = list;
            adapter.replaceAll(list);
            tvNoConsumers.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            progressLayout.setVisibility(View.GONE);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while retrieving consumer entries: " + err)
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
        List<Consumer> filteredList = filter(consumerList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Consumer> filter(List<Consumer> list, String query) {
        List<Consumer> cList = new ArrayList<>();
        String str = query.toLowerCase();
        for (Consumer consumer : list) {
            if (consumer.consumerName.toLowerCase().contains(str)) {
                cList.add(consumer);
            }
        }
        return cList;
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
