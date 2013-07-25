/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.Counters;
import com.sonar.performance.ServerLogs;

public class RestartServer {

  public static Counters execute(Orchestrator orchestrator) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.restartSonar();

    return new Counters().set("Time", StartServer.startupDuration(orchestrator), "ms");
  }

}
