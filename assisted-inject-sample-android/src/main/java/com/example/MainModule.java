package com.example;

import com.squareup.inject.assisted.dagger2.AssistedModule;
import dagger.Module;
import dagger.Provides;

@AssistedModule
@Module(includes = AssistedInject_MainModule.class)
public abstract class MainModule {
  @Provides static @Exclamation String provideExclamation() {
    return "!";
  }
}
