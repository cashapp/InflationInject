package com.squareup.inject.inflation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;
import javax.inject.Inject;

public final class InflationInjectFactory implements LayoutInflater.Factory2 {
  private final Map<String, ViewFactory> factories;
  private final @Nullable LayoutInflater.Factory delegate;
  private final @Nullable LayoutInflater.Factory2 delegate2;

  @Inject
  public InflationInjectFactory(@NonNull Map<String, ViewFactory> factories) {
    this(factories, null);
  }

  @SuppressWarnings("ConstantConditions") // Validating API invariants.
  public InflationInjectFactory(@NonNull Map<String, ViewFactory> factories,
      @Nullable LayoutInflater.Factory delegate) {
    if (factories == null) throw new NullPointerException("factories == null");
    this.factories = factories;
    this.delegate = delegate;
    this.delegate2 = null;
  }

  @SuppressWarnings("ConstantConditions") // Validating API invariants.
  public InflationInjectFactory(@NonNull Map<String, ViewFactory> factories,
      @Nullable LayoutInflater.Factory2 delegate) {
    if (factories == null) throw new NullPointerException("factories == null");
    this.factories = factories;
    this.delegate = null;
    this.delegate2 = delegate;
  }

  @Nullable @Override public View onCreateView(@NonNull String name, @NonNull Context context,
      @Nullable AttributeSet attrs) {
    ViewFactory factory = factories.get(name);
    if (factory != null) {
      return factory.create(context, attrs);
    }
    if (delegate != null) {
      return delegate.onCreateView(name, context, attrs);
    }
    return null;
  }

  @Nullable
  @Override
  public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
    ViewFactory factory = factories.get(name);
    if (factory != null) {
      return factory.create(context, attrs);
    }
    if (delegate2 != null) {
      return delegate2.onCreateView(parent, name, context, attrs);
    }
    if (delegate != null) {
      return delegate.onCreateView(name, context, attrs);
    }
    return null;
  }
}
