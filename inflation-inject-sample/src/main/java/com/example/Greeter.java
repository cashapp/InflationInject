package com.example;

import javax.inject.Inject;

public final class Greeter {
  @Inject Greeter() {
  }

  public String sayHi(String name) {
    return "Hello, " + name + "!";
  }
}
