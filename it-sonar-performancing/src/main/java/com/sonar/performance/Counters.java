/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import java.util.ArrayList;
import java.util.List;

public class Counters {
  private List<Counter> list = new ArrayList<Counter>();

  public Counters set(String name, Long value, String unit) {
    if (value != null) {
      list.add(new Counter(name, value, unit));
    }
    return this;
  }

  public List<Counter> all() {
    return list;
  }
}
