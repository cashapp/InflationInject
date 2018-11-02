package com.example;

import com.squareup.inject.inflation.InflationInjectFactory;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.MainActivityAppCompatDelegate;
import android.view.View;

import dagger.Component;

public final class MainActivityAppCompat extends AppCompatActivity {
    private MainAppCompatComponent mainComponent;

    private AppCompatDelegate appCompatDelegate;

    @Override protected void onCreate(Bundle savedInstanceState) {
        mainComponent = DaggerMainActivityAppCompat_MainAppCompatComponent.create();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.custom_view);

        findViewById(R.id.openAppCompat).setVisibility(View.GONE);
    }

    @NonNull
    @Override
    public AppCompatDelegate getDelegate() {
        if (appCompatDelegate == null) {
            appCompatDelegate = new MainActivityAppCompatDelegate(this, mainComponent.factory());
        }
        return appCompatDelegate;
    }

    @Component(modules = ViewModule.class)
    interface MainAppCompatComponent {
        InflationInjectFactory factory();
    }
}
