package com.example;

import com.squareup.inject.assisted.dagger2.AssistedModule;
import com.squareup.inject.inflation.InflationModule;
import dagger.Module;
import dagger.Provides;

@AssistedModule
@InflationModule
@Module(includes = {
    InflationInject_MainModule.class,
    AssistedInject_MainModule.class
})
public abstract class MainModule {
  @Provides static @Exclamation String provideExclamation() {
    return "!";
  }
}
