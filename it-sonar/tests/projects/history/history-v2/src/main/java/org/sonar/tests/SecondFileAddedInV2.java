package org.sonar.tests;

public class SecondFileAddedInV2 {

  public void hasOneViolation() {
    int i = 0; // unused local variable
    i++;
  }

}
