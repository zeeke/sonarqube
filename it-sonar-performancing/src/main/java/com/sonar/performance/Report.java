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
    Map<String, Map<String, Counter>> countersByKeyAndVersion = new HashMap<String, Map<String, Counter>>();

    Action(String name) {
      this.name = name;
    }

    Action addCounters(String version, Counters counters) {
      for (Counter counter : counters.all()) {
        addCounter(version, counter);
      }
      return this;
    }

    Action addCounter(String version, Counter counter) {
      Map<String, Counter> byVersion = countersByKeyAndVersion.get(counter.name());
      if (byVersion == null) {
        byVersion = new HashMap<String, Counter>();
        countersByKeyAndVersion.put(counter.name(), byVersion);
      }
      byVersion.put(version, counter);
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
      csv.append(version).append(",,");
    }
    csv.append("\n");

    for (Action action : actionsByName.values()) {
      for (Map.Entry<String, Map<String, Counter>> countersByKey : action.countersByKeyAndVersion.entrySet()) {
        String counterName = countersByKey.getKey();
        Map<String, Counter> valuesByVersion = countersByKey.getValue();
        csv.append(action.name).append(",");
        csv.append(counterName).append(",");
        for (String version : versions) {
          Counter counter = valuesByVersion.get(version);
          if (counter == null) {
            csv.append(",,");
          } else {
            csv.append(counter.val()).append(",").append(counter.unit()).append(",");
          }
        }
        csv.append("\n");
      }
    }

    File file = new File("target/sonar-performances.csv");
    FileUtils.write(file, csv.toString(), "UTF-8", false);
    System.out.println("Report exported to: " + file.getAbsolutePath());
  }
}
