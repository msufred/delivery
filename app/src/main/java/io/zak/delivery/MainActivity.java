package io.zak.delivery;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.delivery.data.AppDatabaseImpl;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        new Handler().postDelayed(() -> {
            Log.d(TAG, "Checking user login...");
            // if no user login, check database if any user exits, if no user exist, go to Register activity
            if (Utils.getLoginId(this) == -1) {
                disposables.add(Single.fromCallable(() -> AppDatabaseImpl.getInstance(getApplicationContext()).users().count())
                        .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(size -> {
                            if (size > 0) showLogin();
                            else showRegister();
                        }, err -> {
                            throw new RuntimeException("Database Error:\n" + err);
                        }));
            } else {
                // user login exists, go to Home activity
                showHome();
            }
        }, 2000);
    }

    private void showLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }

    private void showHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}