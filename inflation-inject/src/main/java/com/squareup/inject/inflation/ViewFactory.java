package com.squareup.inject.inflation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ViewFactory<V extends View> {
  @NonNull
  V create(@NonNull Context context, @Nullable AttributeSet attrs);
}
