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
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class FileSystemTest extends PerfTestCase {

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
  public void indexProjectWith100BigFilesXmx128() throws IOException {
    run(128, 3600L);
  }

  @Test
  public void indexProjectWith100BigFilesXmx256() throws IOException {
    run(256, 3600L);
  }

  @Test
  public void indexProjectWith100BigFilesXmx512() throws IOException {
    run(512, 3600L);
  }

  private void run(int xmx, long expectedDuration) throws IOException {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    int nbFiles = 100;
    int lines = 1000;
    for (int nb = 1; nb <= nbFiles; nb++) {
      File xooFile = new File(srcDir, "sample" + nb + ".xoo");
      FileUtils.write(xooFile, StringUtils.repeat(StringUtils.repeat("a", 100) + "\n", lines));
    }

    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "filesystem",
        "sonar.projectName", "filesystem",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "src",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx" + xmx + "m -server -XX:MaxPermSize=64m")
      .setRunnerVersion("2.4")
      .setProjectDir(baseDir);

    orchestrator.executeBuild(runner);

    Properties prof = readProfiling(baseDir, "filesystem");
    assertDurationAround(collector, Long.valueOf(prof.getProperty("Index filesystem and store sources")), expectedDuration);
  }

}
