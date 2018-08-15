package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import com.squareup.inject.inflation.DaggerLayoutInflaterFactory;
import dagger.Component;

public final class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    DaggerLayoutInflaterFactory factory = DaggerMainActivity_MainComponent.create().factory();
    getLayoutInflater().setFactory(factory);

    setContentView(R.layout.custom_view);
  }

  @Component(modules = ViewModule.class)
  interface MainComponent {
    DaggerLayoutInflaterFactory factory();
  }
}
