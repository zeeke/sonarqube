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
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;
import org.sonar.wsclient.services.ServerSetup;
import org.sonar.wsclient.services.ServerSetupQuery;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StartServer {

  public static Counters execute(Orchestrator orchestrator) throws Exception {
    Counters counters = new Counters();

    ServerLogs.clear(orchestrator);
    orchestrator.start();

    if (requiresUpgrade(orchestrator)) {
      upgrade(orchestrator, counters);
    } else {
      long duration = startupDuration(orchestrator);
      counters.set("Time", duration, "ms");
    }
    return counters;
  }

  private static void upgrade(Orchestrator orchestrator, Counters counters) throws IOException {
    ServerLogs.clear(orchestrator);
    long start = System.currentTimeMillis();

    ServerSetupQuery query = new ServerSetupQuery();
    // 30 minutes
    query.setTimeoutMilliseconds(30 * 60 * 1000);
    ServerSetup setup = orchestrator.getServer().getAdminWsClient().create(query);
    if (!setup.isSuccessful()) {
      throw new IllegalStateException("Fail to upgrade");
    }

    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Collections.reverse(lines);
    long end = ServerLogs.extractFirstDate(lines).getTime();
    counters.set("Upgrade Time", (end - start), "ms");
  }

  static long startupDuration(Orchestrator orchestrator) throws IOException {
    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    Date start = ServerLogs.extractFirstDate(lines);
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime() - start.getTime();
  }

  private static boolean requiresUpgrade(Orchestrator orchestrator) {
    Server server = orchestrator.getServer().getAdminWsClient().find(new ServerQuery());
    return server.getStatus() != Server.Status.UP;
  }
}
