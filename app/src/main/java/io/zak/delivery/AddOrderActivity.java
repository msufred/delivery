package io.zak.delivery;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.adapters.ConsumerSpinnerAdapter;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.Consumer;
import io.zak.delivery.data.entities.Order;
import io.zak.delivery.data.relations.OrderDetail;

public class AddOrderActivity extends AppCompatActivity {

    private static final String TAG = "AddOrder";

    // widgets
    private RadioButton rbNewConsumer, rbExistingConsumer;
    private EditText etOrNo, etDate, etConsumerName, etAddress, etContact, etEmail;
    private Spinner consumerSpinner;
    private TextView tvNoConsumers;
    private ImageButton btnSetDate;
    private Button btnSave, btnCancel;
    private LinearLayout newConsumerGroup;
    private RelativeLayout existingConsumerGroup;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private DatePickerDialog datePickerDialog;

    private ConsumerSpinnerAdapter adapter;
    private List<Consumer> consumerList;

    // reference for checking OR NO
    private List<OrderDetail> orders;

    private Date mDateOrdered;
    private Consumer mConsumer; // if null, that means we create new consumer entry

    // fetch from Intent via getIntent()
    private String userId;
    private int vehicleId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        rbNewConsumer = findViewById(R.id.rb_new_consumer);
        rbExistingConsumer = findViewById(R.id.rb_existing_consumer);
        newConsumerGroup = findViewById(R.id.new_consumer_group);
        existingConsumerGroup = findViewById(R.id.existing_consumer_group);
        etOrNo = findViewById(R.id.et_or_no);
        etDate = findViewById(R.id.et_date);
        etConsumerName = findViewById(R.id.et_consumer_name);
        etAddress = findViewById(R.id.et_address);
        etContact = findViewById(R.id.et_contact);
        etEmail = findViewById(R.id.et_email);
        consumerSpinner = findViewById(R.id.consumer_spinner);
        tvNoConsumers = findViewById(R.id.empty_consumer_spinner);
        btnSetDate = findViewById(R.id.btn_pick_date);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        progressGroup = findViewById(R.id.progress_group);

        datePickerDialog = new DatePickerDialog(this);
        datePickerDialog.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            mDateOrdered = calendar.getTime();
            etDate.setText(Utils.dateFormat.format(mDateOrdered));
        });
    }

    private void setListeners() {
        rbNewConsumer.setOnClickListener(v -> {
            if (rbNewConsumer.isChecked()) {
                mConsumer = null;
                newConsumerGroup.setVisibility(View.VISIBLE);
                existingConsumerGroup.setVisibility(View.GONE);
            }
        });
        rbExistingConsumer.setOnClickListener(v -> {
            if (rbExistingConsumer.isChecked()) {
                newConsumerGroup.setVisibility(View.GONE);
                existingConsumerGroup.setVisibility(View.VISIBLE);
            }
        });
        consumerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (consumerList != null) mConsumer = consumerList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        btnSetDate.setOnClickListener(v -> {
            datePickerDialog.show();
        });
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // get user & vehicle id
        userId = getIntent().getStringExtra("user_id");
        vehicleId = getIntent().getIntExtra("vehicle_id", -1);
        if (userId == null || vehicleId == -1) {
            Log.d(TAG, "invalid action");
            goBack();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "fetch consumer entries");
            return AppDatabaseImpl.getInstance(getApplicationContext()).consumers().getAll();
        }).flatMap(consumers ->  {
            Log.d(TAG, "returned with list size=" + consumers.size());
            consumerList = consumers;
            return Single.fromCallable(() -> {
                Log.d(TAG, "fetch order entries");
                return AppDatabaseImpl.getInstance(getApplicationContext()).orders().getOrdersWithDetail();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "returned with list size=" + list.size());
            progressGroup.setVisibility(View.GONE);
            orders = list;

            // setup consumer spinner
            consumerSpinner.setAdapter(new ConsumerSpinnerAdapter(this, consumerList));
            tvNoConsumers.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

            // set default date
            mDateOrdered = new Date();
            etDate.setText(Utils.dateFormat.format(mDateOrdered));
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private boolean validated() {
        String orNo = etOrNo.getText().toString();
        if (orNo.isBlank()) {
            etOrNo.setError("Required");
            return false;
        }

        // check if OR NO exists
        for (OrderDetail order : orders) {
            if (order.order.orNo.equalsIgnoreCase(orNo)) {
                etOrNo.setError("Tracking Number already exists");
                return false;
            }
        }
        return true;
    }

    private void saveAndClose() {
        // save consumer first, if new
        if (mConsumer == null) {
            Consumer consumer = new Consumer();
            consumer.consumerName = Utils.normalize(etConsumerName.getText().toString());
            consumer.consumerAddress = Utils.normalize(etAddress.getText().toString());
            consumer.consumerEmail = Utils.normalize(etEmail.getText().toString());
            consumer.consumerContactNo = Utils.normalize(etContact.getText().toString());

            progressGroup.setVisibility(View.VISIBLE);
            disposables.add(Single.fromCallable(() -> {
                Log.d(TAG, "saving new consumer entry");
                return AppDatabaseImpl.getInstance(getApplicationContext()).consumers().insert(consumer);
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
                progressGroup.setVisibility(View.GONE);
                Log.d(TAG, "returned with id=" + id.intValue());
                saveOrder(id.intValue());
            }, err -> {
                progressGroup.setVisibility(View.GONE);
                Log.e(TAG, "database error: " + err);
            }));
        } else {
            saveOrder(mConsumer.consumerId);
        }
    }

    private void saveOrder(int consumerId) {
        Order order = new Order();
        order.orNo = Utils.normalize(etOrNo.getText().toString());
        order.dateOrdered = mDateOrdered.getTime();
        order.fkVehicleId = vehicleId;
        order.fkConsumerId = consumerId;
        order.userId = userId;
        order.orderStatus = "Processing";
        order.totalAmount = 0;

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "saving order entry");
            return AppDatabaseImpl.getInstance(getApplicationContext()).orders().insert(order);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "returned with id=" + id.intValue());
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "database error" + err);
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
        disposables.dispose();
    }
}
