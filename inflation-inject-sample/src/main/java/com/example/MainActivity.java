package com.example;

import android.app.Activity;
import android.os.Bundle;
import com.squareup.inject.inflation.InflationInjectFactory;
import dagger.Component;

public final class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    InflationInjectFactory factory = DaggerMainActivity_MainComponent.create().factory();
    getLayoutInflater().setFactory(factory);

    setContentView(R.layout.greeter_view);
  }

  @Component(modules = MainModule.class)
  interface MainComponent {
    InflationInjectFactory factory();
  }
}
