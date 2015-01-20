/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.sanity;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class SanityTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .setContext("/")
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
  }

  @Test
  public void sanity_check() {
    scan("shared/xoo-sample");

    Sonar sonar = orchestrator.getServer().getWsClient();
    assertThat(sonar.findAll(new ResourceQuery().setQualifiers("TRK"))).hasSize(1);

    // At least one measure
    Resource master = getResource("sample");
    assertThat(master.getName()).isEqualTo("Sample");
    assertThat(master.getMeasure("lines")).isNotEqualTo(0);

    // At least one issue
    assertThat(orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).size()).isNotEqualTo(0);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines"));
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuild(runner);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      .setProfile("one-issue-per-line")
      .setProperties(props);
    return runner;
  }

}
