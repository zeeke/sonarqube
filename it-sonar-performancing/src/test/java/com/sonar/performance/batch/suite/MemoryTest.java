/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch.suite;

import com.google.common.base.Strings;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.performance.MavenLogs;
import com.sonar.performance.PerfTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.PropertyCreateQuery;

import java.io.File;
import java.io.IOException;

public class MemoryTest extends PerfTestCase {

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = BatchPerfTestSuite.ORCHESTRATOR;

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void should_not_fail_with_limited_xmx_memory_and_no_coverage_per_test() {
    orchestrator.executeBuild(
      newSonarRunner("-Xmx80m -server -XX:-HeapDumpOnOutOfMemoryError")
      );
  }

  int DEPTH = 4;

  // Property on root module is duplicated in each module so it may be big
  @Test
  public void analyzeProjectWithManyModulesAndBigProperties() throws IOException {

    File baseDir = temp.newFolder();

    prepareModule(baseDir, "moduleA", 1);
    prepareModule(baseDir, "moduleB", 1);
    prepareModule(baseDir, "moduleC", 1);

    FileUtils.write(new File(baseDir, "sonar-project.properties"), "sonar.modules=moduleA,moduleB,moduleC\n", true);
    FileUtils.write(new File(baseDir, "sonar-project.properties"), "sonar.myBigProp=" + Strings.repeat("A", 1000), true);

    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "big-module-tree",
        "sonar.projectName", "Big Module Tree",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server -XX:MaxPermSize=64m")
      .setRunnerVersion("2.4")
      .setProjectDir(baseDir);

    BuildResult result = orchestrator.executeBuild(runner);
    assertDurationAround(collector, MavenLogs.extractTotalTime(result.getLogs()), 25000L);

    // Second execution with a property on server side
    orchestrator.getServer().getAdminWsClient().create(new PropertyCreateQuery("sonar.anotherBigProp", Strings.repeat("B", 1000), "big-module-tree"));
    result = orchestrator.executeBuild(runner);
    assertDurationAround(collector, MavenLogs.extractTotalTime(result.getLogs()), 29000L);
  }

  private void prepareModule(File parentDir, String moduleName, int depth) throws IOException {
    File moduleDir = new File(parentDir, moduleName);
    moduleDir.mkdir();
    File projectProps = new File(moduleDir, "sonar-project.properties");
    FileUtils.write(projectProps, "sonar.moduleKey=" + moduleName + "\n", true);
    if (depth < DEPTH) {
      FileUtils.write(projectProps, "sonar.modules=" + moduleName + "A," + moduleName + "B," + moduleName + "C\n", true);
      prepareModule(moduleDir, moduleName + "A", depth + 1);
      prepareModule(moduleDir, moduleName + "B", depth + 1);
      prepareModule(moduleDir, moduleName + "C", depth + 1);
    }
  }

}
