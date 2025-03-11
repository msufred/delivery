package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.data.AppDatabaseImpl;
import io.zak.delivery.data.entities.User;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "Register";

    private EditText etUsername, etPassword, etConfirmPassword;
    private ProgressBar progressBar;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_password_confirm);
        Button btnRegister = findViewById(R.id.btn_register);
        Button btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_circular);

        // listeners
        btnRegister.setOnClickListener(v -> {
            if (validated()) register();
        });

        btnLogin.setOnClickListener(v -> showLogin());

        dialogBuilder = new AlertDialog.Builder(this);
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

        String password = etPassword.getText().toString();
        String repassword = etConfirmPassword.getText().toString();

        if (password.isBlank()) {
            etPassword.setError("Required");
            isValid = false;
        }

        if (repassword.isBlank() || !repassword.equals(password)) {
            etConfirmPassword.setError("Required or Don't Match");
            isValid = false;
        }
        return isValid;
    }

    private void register() {
        String username = Utils.normalize(etUsername.getText().toString());
        String password = Utils.normalize(etPassword.getText().toString());

        User user = new User();
        user.username = username;
        user.password = password;

        progressBar.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Registering user");
            return AppDatabaseImpl.getInstance(getApplicationContext()).users().insert(user);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressBar.setVisibility(View.INVISIBLE);
            Log.d(TAG, "Returned with ID=" + id.intValue());
            if (id.intValue() != -1) {
                Toast.makeText(this, "User registered.", Toast.LENGTH_SHORT).show();
                Utils.saveLoginId(getApplicationContext(), id.intValue());
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                dialogBuilder.setTitle("Invalid")
                        .setMessage("Failed to register user")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            }
        }, err -> {
            progressBar.setVisibility(View.INVISIBLE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while registering user: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void showLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
