package org.sonar.tests;

public class Hello {
  private String hello;

  public Hello(String s) {
    this.hello = s;
  }

  public String say() {
    if (hello != null) {
      return hello;
    }
    return "";
  }

  // this method is not tested
  public String cry() {
    return "ouinnnn";
  }
}
