/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.performance.PerfTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class DuplicationTest extends PerfTestCase {

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = BatchPerfTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() throws IOException {
    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newSonarRunner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
  }

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * SONAR-3060
   */
  @Test
  public void hugeJavaFile() {
    MavenBuild build = MavenBuild.create(FileLocation.of("projects/huge-file/pom.xml").getFile())
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);
    Resource file = getResource("com.sonarsource.it.samples:huge-file:src/main/java/huge/HugeFile.java");
    assertThat(file.getMeasureValue("duplicated_lines")).isGreaterThan(50000.0);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(key, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density", "useless-duplicated-lines"));
  }
}
