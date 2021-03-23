package com.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.squareup.inject.inflation.Inflated;
import com.squareup.inject.inflation.InflationInject;

@SuppressLint("ViewConstructor") // Created by Inflation Inject.
public final class CustomView extends LinearLayout {
  private final Greeter greeter;

  @InflationInject
  public CustomView(@Inflated Context context, @Inflated AttributeSet attrs, Greeter greeter) {
    super(context, attrs);
    this.greeter = greeter;
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    EditText input = findViewById(R.id.input);
    TextView output = findViewById(R.id.output);

    findViewById(R.id.show).setOnClickListener(view -> {
      String inputText = input.getText().toString();
      String outputText = greeter.sayHi(inputText);
      output.setText(outputText);
    });
  }
}
