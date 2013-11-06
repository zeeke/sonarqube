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

public class ServerTest extends PerfTestCase {

  @Test
  public void server_startup() throws Exception {
    Orchestrator orchestrator = Orchestrator.builderEnv().build();
    try {
      ServerLogs.clear(orchestrator);
      orchestrator.start();
      assertDuration(startupDuration(orchestrator), 23000);
    } finally {
      orchestrator.stop();
    }
  }


  static long startupDuration(Orchestrator orchestrator) throws IOException {
    // compare dates of first and last log
    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Date start = ServerLogs.extractFirstDate(lines);
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime() - start.getTime();
  }
}
