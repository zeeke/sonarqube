/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.issue.IssueQuery;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class ServerProfilingLogsTest {

  @Test
  public void should_disable_profiling_logs() throws IOException {
    String serverLogs = startServerDoQueryThenStopAndGetLogs("NONE");
    assertThat(serverLogs).doesNotContain("[http]");
    //assertThat(serverLogs).doesNotContain("[es]");
  }

  @Test
  public void should_enable_basic_profiling_logs() throws IOException {
    String serverLogs = startServerDoQueryThenStopAndGetLogs("BASIC");
    assertThat(serverLogs).contains("[http]");
    //assertThat(serverLogs).doesNotContain("[es]");
  }

  @Test
  public void should_enable_full_profiling_logs() throws IOException {
    String serverLogs = startServerDoQueryThenStopAndGetLogs("FULL");
    assertThat(serverLogs).contains("[http]");
    //assertThat(serverLogs).contains("[es]");
  }

  private String startServerDoQueryThenStopAndGetLogs(String profilingLevel) throws IOException {
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .removeDistributedPlugins()
      .addPlugin(ItUtils.xooPlugin())
      .setServerProperty("sonar.log.profilingLevel", profilingLevel).build();
    orchestrator.start();
    orchestrator.getServer().adminWsClient().issueClient().find(IssueQuery.create());
    orchestrator.stop();
    String serverLogs = IOUtils.toString(new FileInputStream(orchestrator.getServer().getLogs()));
    return serverLogs;
  }
}
