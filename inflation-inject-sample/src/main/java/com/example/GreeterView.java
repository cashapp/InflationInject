package com.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.squareup.inject.assisted.Assisted;
import com.squareup.inject.inflation.InflationInject;

@SuppressLint("ViewConstructor") // Created by Inflation Inject.
public final class GreeterView extends LinearLayout {
  private final Greeter greeter;

  @InflationInject
  GreeterView(@Assisted Context context, @Assisted AttributeSet attrs, Greeter.Factory greeter) {
    super(context, attrs);

    TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GreeterView, 0,
        R.style.GreeterViewDefaults);
    String greeting = array.getString(R.styleable.GreeterView_greeting);
    array.recycle();

    this.greeter = greeter.create(greeting);
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
