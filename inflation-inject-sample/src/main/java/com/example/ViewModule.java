package com.example;

import com.squareup.inject.inflation.DaggerLayoutInflaterFactory;
import com.squareup.inject.inflation.InflationModule;
import com.squareup.inject.inflation.ViewFactory;
import dagger.Module;
import dagger.Provides;
import java.util.Map;

@InflationModule
@Module(includes = InflationInject_ViewModule.class)
public abstract class ViewModule {
  @Provides
  static DaggerLayoutInflaterFactory provide(Map<String, ViewFactory> factories) {
    return new DaggerLayoutInflaterFactory(factories, null);
  }
}
