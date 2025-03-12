package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.User;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";

    // widgets
    private ImageView profile;
    private TextView tvUsername, tvPosition, tvLicense;
    private ImageButton btnEdit;
    private CardView cardStocks, cardOrders, cardProducts, cardConsumers;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWidgets();
        setListeners();
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
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnEdit.setOnClickListener(v -> {
            // TODO
        });

        cardStocks.setOnClickListener(v -> {
            // pass vehicle id
            if (mUser.fkVehicleId != -1) {
                Intent intent = new Intent(this, StocksActivity.class);
                intent.putExtra("vehicle_id", mUser.fkVehicleId);
                startActivity(intent);
            } else {
                dialogBuilder.setTitle("Invalid")
                        .setMessage("Vehicle ID not set.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            }
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = Utils.getLoginId(this);
        if (id == -1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Retrieving user info");
            return AppDatabaseImpl.getInstance(getApplicationContext()).users().getUserById(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(users -> {
            Log.d(TAG, "Returned with list size=" + users.size());
            progressGroup.setVisibility(View.GONE);
            mUser = users.get(0);
            if (mUser != null) {
                displayInfo(mUser);
            }
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while retrieving user info: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(User user) {
        tvUsername.setText(user.username);
        if (user.position != null) tvPosition.setText(user.position);
        if (user.license != null) tvLicense.setText(user.license);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources");
        disposables.dispose();
    }
}
