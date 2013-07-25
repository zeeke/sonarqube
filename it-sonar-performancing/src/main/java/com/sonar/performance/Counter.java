/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

public class Counter {

  private String name;
  private long val;
  private String unit;

  public Counter(String name, long val, String unit) {
    this.name = name;
    this.val = val;
    this.unit = unit;
  }

  public String name() {
    return name;
  }

  public long val() {
    return val;
  }

  public String unit() {
    return unit;
  }
}
