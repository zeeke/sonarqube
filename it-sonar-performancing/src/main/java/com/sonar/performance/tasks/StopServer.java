/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.Counters;
import com.sonar.performance.PerformanceTask;
import com.sonar.performance.ServerLogs;
import org.apache.commons.io.FileUtils;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StopServer extends PerformanceTask {

  public StopServer(String name) {
    super(name);
  }

  @Override
  public int replay() {
    return 1;
  }

  public void execute(Orchestrator orchestrator, Counters counters) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.stop();

    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Collections.reverse(lines);

    Pattern pattern = Pattern.compile(".*Stop sonar done: (\\d++) ms.*");
    for (String line : lines) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        long duration = Long.parseLong(matcher.group(1));
        counters.set("Time (ms)", duration);
      }
    }
  }
}
