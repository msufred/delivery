package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.zak.delivery.firebase.UserEntry;

public class ViewProfileActivity extends AppCompatActivity {

    private static final String TAG = "Profile";

    // Widgets
    private TextView tvUsername, tvPosition, tvContact, tvEmail, tvAddress;
    private ImageButton btnEdit;
    private Button btnLogout;
    private RelativeLayout progressGroup;

    private AlertDialog.Builder dialogBuilder;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private UserEntry mUserEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getWidgets() {
        tvUsername = findViewById(R.id.tv_username);
        tvPosition = findViewById(R.id.tv_position);
        tvContact = findViewById(R.id.tv_contact);
        tvAddress = findViewById(R.id.tv_address);
        tvEmail = findViewById(R.id.tv_email);
        btnEdit = findViewById(R.id.btn_edit);
        btnLogout = findViewById(R.id.btn_logout);
        progressGroup = findViewById(R.id.progress_group);
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnEdit.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference(); // root
        progressGroup.setVisibility(View.VISIBLE);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mDatabase.child("users")
                    .child(user.getUid())
                    .get()
                    .addOnCompleteListener(this, task -> {
                        progressGroup.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            mUserEntry = task.getResult().getValue(UserEntry.class);
                            displayInfo(mUserEntry);
                        }
                    });
        }
    }

    private void displayInfo(UserEntry user) {
        Log.d(TAG, String.valueOf(user == null));
        if (user != null) {
            tvUsername.setText(user.fullName);
            if (user.position != null) tvPosition.setText(user.position);
            if (user.address != null) tvAddress.setText(user.address);
            if (user.email != null) tvEmail.setText(user.email);
            if (user.contactNo != null) tvContact.setText(user.contactNo);
        }
    }

    private void logout() {
        dialogBuilder.setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Confirm", (dialog, which) -> {
                    dialog.dismiss();
                    mAuth.signOut(); // sign out
                    Log.d(TAG, "User signed out");
                    startActivity(new Intent(this, MainActivity.class));
                });
        dialogBuilder.create().show();
    }
}