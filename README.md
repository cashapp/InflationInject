# Inflation Injection

Constructor-inject views during XML layout inflation.

Looking for Assisted Inject? It's [built in to Dagger now](https://dagger.dev/dev-guide/assisted-injection.html)!


## Usage

Write your layout XML like normal.

```xml
<LinearLayout>
  <com.example.CustomView/>
  <TextView/>
</LinearLayout>
```

Use `@InflationInject` in `CustomView`:

```java
public final class CustomView {
  private final Picasso picasso;
  
  @InflationInject
  public CustomView(
    @Inflated Context context,
    @Inflated AttributeSet attrs,
    Picasso picasso
  ) {
    super(context, attrs);
    this.picasso = picasso;
  }
  
  // ...
}
```

In order to allow Dagger to create your custom views, add `@InflationModule` to a Dagger module and
add the generated module name to its `includes=`.

```java
@InflationModule
@Module(includes = InflationInject_PresenterModule.class)
abstract class PresenterModule {}
```

The annotation processor will generate the `InflationInject_PresenterModule` for us. It will not be
resolved until the processor runs.

Finally, inject `InflationInjectFactory` and add it to your `LayoutInflater`.

```java
InflationInjectFactory factory = DaggerMainActivity_MainComponent.create().factory();
getLayoutInflater().setFactory(factory);

setContentView(R.layout.main_view);
```


## Download

```groovy
repositories {
  mavenCentral()
}
dependencies {
  implementation 'app.cash.inject:inflation-inject:1.0.0'
  annotationProcessor 'app.cash.inject:inflation-inject-processor:1.0.0'
}
```

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
repositories {
  maven {
    url 'https://oss.sonatype.org/content/repositories/snapshots/'
  }
}
dependencies {
  implementation 'app.cash.inject:inflation-inject:1.1.0-SNAPSHOT'
  annotationProcessor 'app.cash.inject:inflation-inject-processor:1.1.0-SNAPSHOT'
}
```

</p>
</details>


# License

    Copyright 2017 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

