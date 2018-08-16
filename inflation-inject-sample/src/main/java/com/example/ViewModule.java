package com.example;

import com.squareup.inject.inflation.InflationModule;
import dagger.Module;

@InflationModule
@Module(includes = InflationInject_ViewModule.class)
public abstract class ViewModule {
}
