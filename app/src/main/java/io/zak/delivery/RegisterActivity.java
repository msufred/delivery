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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.zak.delivery.firebase.UserEntry;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "Register";

    private EditText etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressBar;
    private AlertDialog.Builder dialogBuilder;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_password_confirm);
        Button btnRegister = findViewById(R.id.btn_register);
        Button btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_circular);
        progressBar.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();

        // listeners
        btnRegister.setOnClickListener(v -> {
            if (validated()) register();
        });

        btnLogin.setOnClickListener(v -> showLogin());

        dialogBuilder = new AlertDialog.Builder(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    private boolean validated() {
        boolean isValid = true;
        if (etEmail.getText().toString().isBlank()) {
            etEmail.setError("Required");
            isValid = false;
        }

        String password = etPassword.getText().toString();
        String passwordConfirm = etConfirmPassword.getText().toString();

        if (password.isBlank()) {
            etPassword.setError("Required");
            isValid = false;
        }

        if (passwordConfirm.isBlank() || !passwordConfirm.equals(password)) {
            etConfirmPassword.setError("Required or Don't Match");
            isValid = false;
        }
        return isValid;
    }

    private void register() {
        String email = Utils.normalize(etEmail.getText().toString());
        String password = Utils.normalize(etPassword.getText().toString());

        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Registering user");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "User Registered", Toast.LENGTH_SHORT).show();
                        createUserEntry(email);
                    } else {
                        Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Registration failure", task.getException());
                    }
                });
    }

    private void createUserEntry(String email) {
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser != null) {
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "creating new user entry [Firebase]");
            UserEntry userEntry = new UserEntry();
            userEntry.fullName = "New User";
            userEntry.position = "Employee";
            userEntry.email = email;

            // get Firebase database reference
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child("users").child(fUser.getUid()).setValue(userEntry)
                    .addOnCompleteListener(this, task -> {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "user created");
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "user creation failure", task.getException());
                            dialogBuilder.setTitle("Database Error")
                                    .setMessage("Error while creating user entry: " + task.getException())
                                    .setPositiveButton("Dismiss", (dialog, which) -> {
                                        dialog.dismiss();
                                    });
                            dialogBuilder.create().show();
                        }
                    });
        }
    }

    private void showLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

}
