/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.server;

import com.sonar.performance.PerfTestCase;
import com.sonar.performance.ServerLogs;

import com.sonar.orchestrator.Orchestrator;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ElasticSynchronizationTest extends PerfTestCase {

  @Test
  public void server_startup_and_shutdown() throws Exception {
    String defaultWebJavaOptions = "-Xmx768m -XX:MaxPermSize=160m -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.management.enabled=false";
    String defaultSearchJavaOptions = "-Xmx1024m -Xms1024m -Xss256k -Djava.net.preferIPv4Stack=true"+
      " -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75"+
      " -XX:+UseCMSInitiatingOccupancyOnly -XX:+HeapDumpOnOutOfMemoryError"+
      " -Djava.awt.headless=true";

    Orchestrator orchestrator = Orchestrator.builderEnv()
      // See http://wiki.apache.org/tomcat/HowTo/FasterStartUp
      // Sometimes source of entropy is too small and Tomcat spends ~20 seconds on the step :
      // "Creation of SecureRandom instance for session ID generation using [SHA1PRNG]"
      // Using /dev/urandom fixes the issue on linux
      // Set the required params for ES
      .setServerProperty("sonar.web.javaOpts", defaultWebJavaOptions + " -Djava.security.egd=file:/dev/./urandom")
      .setServerProperty("sonar.search.javaOpts", defaultSearchJavaOptions)
      .setServerProperty("sonar.issues.use_es_backend","true")
      .addServerJvmArgument("-server")
        // Keep the DB as we're testing its synchronization time
      .setOrchestratorProperty("orchestrator.keepDatabase", "true")

      .build();
    try {
      long startupDuration = start(orchestrator);
      System.out.printf("Server started in %d ms\n", startupDuration);
      assertDurationLessThan(startupDuration, 2 * 60 * 60 * 1000 );
    } finally {
      orchestrator.stop();
    }
  }

  long start(Orchestrator orchestrator) throws IOException {
    ServerLogs.clear(orchestrator);
    orchestrator.start();
    return logsPeriod(orchestrator);
  }

  long stop(Orchestrator orchestrator) throws Exception {
    ServerLogs.clear(orchestrator);
    orchestrator.stop();
    return logsPeriod(orchestrator);
  }

  private long logsPeriod(Orchestrator orchestrator) throws IOException {
    // compare dates of first and last log
    List<String> lines = FileUtils.readLines(orchestrator.getServer().getLogs());
    if (lines.size() < 2) {
      throw new IllegalStateException("Fail to estimate server shutdown or startup duration. Not enough logs.");
    }
    Date start = ServerLogs.extractFirstDate(lines);
    Collections.reverse(lines);
    Date end = ServerLogs.extractFirstDate(lines);
    return end.getTime() - start.getTime();
  }
}
