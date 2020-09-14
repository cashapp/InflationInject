Assisted Injection for JSR 330
==============================

Manually injected dependencies for your JSR 330 configuration. More about assisted injections in
the [Guice wiki](https://github.com/google/guice/wiki/AssistedInject).


Usage
-----

```java
class MyPresenter {
  @AssistedInject
  MyPresenter(Long foo, @Assisted String bar) {}
  
  @AssistedInject.Factory
  interface Factory {
    MyPresenter create(String bar);
  }
}
```

This will generate the following:

```java
public final class MyPresenter_AssistedFactory implements MyPresenter.Factory {
  private final Provider<Long> foo;

  @Inject public MyPresenter_AssistedFactory(Provider<Long> foo) {
    this.foo = foo;
  }

  @Override public MyPresenter create(String bar) {
    return new MyPresenter(foo.get(), bar);
  }
}
```


Usage with Dagger 2
-------------------

In order to allow Dagger to use the generated factory, define an assisted dagger module anywhere in
the same gradle module:

```java
@AssistedModule
@Module(includes = AssistedInject_PresenterModule.class)
abstract class PresenterModule {}
```

The library will generate the `AssistedInject_PresenterModule` for us. 


Download
--------

```groovy
compileOnly 'com.squareup.inject:assisted-inject-annotations:0.6.0'
annotationProcessor 'com.squareup.inject:assisted-inject-processor:0.6.0'
```

With Dagger 2:

```groovy
compileOnly 'com.squareup.inject:assisted-inject-annotations-dagger2:0.6.0'
annotationProcessor 'com.squareup.inject:assisted-inject-processor-dagger2:0.6.0'
```


License
=======

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

