/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import java.util.LinkedHashMap;
import java.util.Map;

public class Counters {
  private Map<String, Long> values = new LinkedHashMap<String, Long>();

  public Counters set(String name, Long value) {
    if (value != null) {
      Long previousValue = values.get(name);
      if (previousValue == null || value < previousValue) {
        values.put(name, value);
      }
    }
    return this;
  }

  public Map<String, Long> values() {
    return values;
  }
}
