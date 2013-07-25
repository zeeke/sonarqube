/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

public abstract class PerformanceTask implements Task {
  private final String name;

  protected PerformanceTask(String name) {
    // Check compatibility with CSV format
    if (name.contains(",")) {
      throw new IllegalArgumentException("Commas are not accepted in task name: " + name);
    }
    this.name = name;
  }

  public final String name() {
    return name;
  }

  public abstract int replay();
}
