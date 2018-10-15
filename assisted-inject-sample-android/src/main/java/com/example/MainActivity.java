package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import dagger.Component;

public final class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_view);

    EditText input = findViewById(R.id.input);
    TextView output = findViewById(R.id.output);

    Greeter.Factory factory = DaggerMainActivity_MainComponent.create().factory();
    Greeter greeter = factory.create("Hello!");
    findViewById(R.id.show).setOnClickListener(view -> {
      String inputText = input.getText().toString();
      String outputText = greeter.sayHi(inputText);
      output.setText(outputText);
    });
  }

  @Component(modules = MainModule.class)
  interface MainComponent {
    Greeter.Factory factory();
  }
}
