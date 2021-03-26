package app.cash.inject.inflation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An interface for abstracting the two-argument {@link View} constructor that is used during
 * XML-based layout inflation.
 */
public interface ViewFactory {
  @NonNull
  View create(@NonNull Context context, @Nullable AttributeSet attrs);
}
