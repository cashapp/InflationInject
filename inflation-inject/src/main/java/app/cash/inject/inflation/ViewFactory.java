package app.cash.inject.inflation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ViewFactory {
  @NonNull
  View create(@NonNull Context context, @Nullable AttributeSet attrs);
}
