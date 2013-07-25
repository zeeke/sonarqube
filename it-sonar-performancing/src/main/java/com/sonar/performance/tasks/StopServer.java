/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.Counters;
import com.sonar.performance.ServerLogs;
import org.apache.commons.io.FileUtils;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StopServer {

  public static Counters execute(Orchestrator orchestrator) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.stop();

    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Collections.reverse(lines);

    Pattern pattern = Pattern.compile(".*Stop sonar done: (\\d++) ms.*");
    for (String line : lines) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        long duration = Long.parseLong(matcher.group(1));
        return new Counters().set("Time", duration, "ms");
      }
    }
    return new Counters();
  }
}
