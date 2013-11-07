/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.automated;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.ServerLogs;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerTest extends PerfTestCase {

  @Test
  public void server_startup_and_shutdown() throws Exception {
    Orchestrator orchestrator = Orchestrator.builderEnv().build();
    try {
      long startupDuration = start(orchestrator);
      assertDurationAround(startupDuration, 43000);

      long shutdownDuration = stop(orchestrator);
      assertDurationAround(shutdownDuration, 2000);

    } finally {
      orchestrator.stop();
    }
  }

  long start(Orchestrator orchestrator) throws IOException {
    ServerLogs.clear(orchestrator);
    orchestrator.start();
    // compare dates of first and last log
    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Date start = ServerLogs.extractFirstDate(lines);
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime() - start.getTime();
  }

  long stop(Orchestrator orchestrator) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.stop();

    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Collections.reverse(lines);

    Pattern pattern = Pattern.compile(".*Stop sonar done: (\\d++) ms.*");
    for (String line : lines) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches()) {
        long duration = Long.parseLong(matcher.group(1));
        return duration;
      }
    }
    throw new IllegalStateException("Fail to estimate shutdown duration");
  }
}
