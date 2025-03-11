package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.User;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Login";

    private EditText etUsername, etPassword;
    private ProgressBar progressBar;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_circular);

        dialogBuilder = new AlertDialog.Builder(this);

        // listeners
        btnLogin.setOnClickListener(v -> {
            if (validated()) login();
        });

        btnRegister.setOnClickListener(v -> {
            showRegister();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        progressBar.setVisibility(View.INVISIBLE);
    }

    private boolean validated() {
        boolean isValid = true;
        if (etUsername.getText().toString().isBlank()) {
            etUsername.setError("Required");
            isValid = false;
        }
        if (etPassword.getText().toString().isBlank()) {
            etPassword.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void login() {
        String username = Utils.normalize(etUsername.getText().toString());
        String password= Utils.normalize(etPassword.getText().toString());
        progressBar.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Log in user.");
            return AppDatabaseImpl.getInstance(getApplicationContext()).users().getUser(username, password);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(users -> {
            progressBar.setVisibility(View.INVISIBLE);
            Log.d(TAG, "Returned with list size=" + users.size());
            User user = users.get(0);
            if (user != null) {
                Utils.saveLoginId(getApplicationContext(), user.id);
                // TODO go to home
                finish();
            } else {
                etUsername.setError("Invalid User");
                etPassword.setError("Invalid Password");
                dialogBuilder.setTitle("Invalid User")
                                .setMessage("Invalid username and/or password. Try again.")
                                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            }
        }, err -> {
            progressBar.setVisibility(View.INVISIBLE);
            Log.e(TAG, "Database error: " + err);
        }));
    }

    private void showRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
