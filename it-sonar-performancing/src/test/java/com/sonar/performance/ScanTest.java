/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.*;

public class ScanTest extends PerfTestCase {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setOrchestratorProperty("sonar.runtimeVersion", "4.0")
    .addPlugin(MavenLocation.create("com.sonarsource.xoo", "sonar-xoo-plugin", "1.0-SNAPSHOT"))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/one-xoo-issue-per-line.xml"))
    .build();

  @BeforeClass
  public static void setUp() {
    // Execute this query in order for next analysis to not freeze (FIXME why does it freeze?)
    orchestrator.getDatabase().truncateInspectionTables();

    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newSonarRunner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
  }

  @Before
  public void cleanDatabase() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void scan_xoo_project() {
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true"
    );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;
    assertDurationAround(duration, 8000L);
  }

  @Test
  public void preview_scan_xoo_project() {
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.dryRun", "true",
      "sonar.showProfiling", "true"
    );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;

    assertDurationAround(duration, 80000L);
  }

  @Test
  public void should_not_fail_with_limited_xmx_memory_and_no_coverage_per_test() {
    orchestrator.executeBuild(
      newSonarRunner("-Xmx80m -server -XX:-HeapDumpOnOutOfMemoryError")
    );
  }

  private static SonarRunner newSonarRunner(String sonarRunnerOpts, String... props) {
    return SonarRunner.create()
      .setProperties(props)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", sonarRunnerOpts)
      .setRunnerVersion("2.3")
      .setProjectDir(FileLocation.of("projects/xoo-sample").getFile());
  }

}
