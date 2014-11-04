/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.performance.PerfTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class BootstrappingTest extends PerfTestCase {

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

  @Test
  public void analyzeProjectWith400FlatModules() throws IOException {
    int SIZE = 400;

    File baseDir = temp.newFolder();
    File projectProps = new File(baseDir, "sonar-project.properties");

    StringBuilder moduleListBuilder = new StringBuilder(SIZE * ("module".length() + 2));

    for (int i = 1; i <= SIZE; i++) {
      moduleListBuilder.append("module").append(i);
      File moduleDir = new File(baseDir, "module" + i);
      moduleDir.mkdir();
      if (i != SIZE) {
        moduleListBuilder.append(",");
      }
    }

    FileUtils.write(projectProps, "sonar.modules=", true);
    FileUtils.write(projectProps, moduleListBuilder.toString(), true);
    FileUtils.write(projectProps, "\n", true);

    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "many-modules",
        "sonar.projectName", "Many Modules",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server -XX:MaxPermSize=64m")
      .setRunnerVersion("2.4")
      .setProjectDir(baseDir);

    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;
    assertDurationAround(duration, 4000L);
  }

}
