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

public class RestartServer extends PerformanceTask {

  public RestartServer(String name) {
    super(name);
  }

  @Override
  public int replay() {
    return 1;
  }

  public void execute(Orchestrator orchestrator, Counters counters) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.restartSonar();
    counters.set("Time (ms)", StartServer.startupDuration(orchestrator));
  }

}
