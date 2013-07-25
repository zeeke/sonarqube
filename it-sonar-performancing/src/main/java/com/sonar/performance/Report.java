/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Report {
  private static final class Action {
    String name;
    Map<String, Map<String, Long>> valuesByKeyAndVersion = new HashMap<String, Map<String, Long>>();

    Action(String name) {
      this.name = name;
    }

    Action addCounters(String version, Counters counters) {
      for (Map.Entry<String, Long> entry : counters.values().entrySet()) {
        String valName = entry.getKey();
        Long value = entry.getValue();
        addCounter(version, valName, value);
      }
      return this;
    }

    Action addCounter(String version, String valName, Long value) {
      Map<String, Long> byVersion = valuesByKeyAndVersion.get(valName);
      if (byVersion == null) {
        byVersion = new HashMap<String, Long>();
        valuesByKeyAndVersion.put(valName, byVersion);
      }
      byVersion.put(version, value);
      return this;
    }
  }

  private final List<String> versions = new ArrayList<String>();
  private final Map<String, Action> actionsByName = new LinkedHashMap<String, Action>();
  private String currentVersion = null;

  public Report setCurrentVersion(String s) {
    currentVersion = s;
    versions.add(s);
    return this;
  }

  public Report add(String actionName, Counters counters) {
    Action action = actionsByName.get(actionName);
    if (action == null) {
      action = new Action(actionName);
      actionsByName.put(actionName, action);
    }
    action.addCounters(currentVersion, counters);
    return this;
  }

  public void dump() throws IOException {
    StringBuilder csv = new StringBuilder();

    // header
    csv.append(",,");
    for (String version : versions) {
      csv.append(version).append(",");
    }
    csv.append("\n");

    // body
    for (Action action : actionsByName.values()) {
      for (Map.Entry<String, Map<String, Long>> valuesByKey : action.valuesByKeyAndVersion.entrySet()) {
        String counterName = valuesByKey.getKey();
        Map<String, Long> valuesByVersion = valuesByKey.getValue();
        csv.append(action.name).append(",");
        csv.append(counterName).append(",");
        for (String version : versions) {
          Long value = valuesByVersion.get(version);
          if (value != null) {
            csv.append(value);
          }
          csv.append(",");
        }
        csv.append("\n");
      }
    }

    // export
    File file = new File("target/sonar-performances.csv");
    FileUtils.write(file, csv.toString(), "UTF-8", false);
    System.out.println("Report exported to: " + file.getAbsolutePath());
  }
}
