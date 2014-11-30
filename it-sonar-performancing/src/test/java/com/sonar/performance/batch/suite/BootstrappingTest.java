/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.performance.MavenLogs;
import com.sonar.performance.PerfTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class BootstrappingTest extends PerfTestCase {

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
        "sonar.projectKey", "many-flat-modules",
        "sonar.projectName", "Many Flat Modules",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server -XX:MaxPermSize=64m")
      .setRunnerVersion("2.4")
      .setProjectDir(baseDir);

    BuildResult result = orchestrator.executeBuild(runner);
    // First analysis
    assertDurationAround(collector, MavenLogs.extractTotalTime(result.getLogs()), 49000L);

    result = orchestrator.executeBuild(runner);
    // Second analysis is longer since we load project referential
    assertDurationAround(collector, MavenLogs.extractTotalTime(result.getLogs()), 60000L);
  }

  @Test
  public void analyzeProjectWith100NestedModules() throws IOException {
    int SIZE = 100;

    File baseDir = temp.newFolder();
    File currentDir = baseDir;

    for (int i = 1; i <= SIZE; i++) {
      File projectProps = new File(currentDir, "sonar-project.properties");
      FileUtils.write(projectProps, "sonar.modules=module" + i + "\n", true);
      if (i >= 1) {
        FileUtils.write(projectProps, "sonar.moduleKey=module" + (i - 1), true);
      }
      File moduleDir = new File(currentDir, "module" + i);
      moduleDir.mkdir();
      currentDir = moduleDir;
    }
    FileUtils.write(new File(currentDir, "sonar-project.properties"), "sonar.moduleKey=module" + SIZE, true);

    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "many-nested-modules",
        "sonar.projectName", "Many Nested Modules",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server -XX:MaxPermSize=64m")
      .setRunnerVersion("2.4")
      .setProjectDir(baseDir);

    BuildResult result = orchestrator.executeBuild(runner);
    // First analysis
    assertDurationAround(collector, MavenLogs.extractTotalTime(result.getLogs()), 8300L);

    result = orchestrator.executeBuild(runner);
    // Second analysis
    assertDurationAround(collector, MavenLogs.extractTotalTime(result.getLogs()), 9400L);
  }

}
