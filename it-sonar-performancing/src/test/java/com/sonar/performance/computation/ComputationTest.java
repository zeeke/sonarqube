/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.computation;

import com.google.common.base.Charsets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.performance.MavenLogs;
import com.sonar.performance.PerfTestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ComputationTest extends PerfTestCase {
  private static int MAX_HEAP_SIZE_IN_MEGA = 600;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator
    .builderEnv()
    .addPlugin(MavenLocation.create("com.sonarsource.xoo", "sonar-xoo-plugin", "1.0-SNAPSHOT"))
    .setServerProperty(
      "sonar.web.javaOpts",
      String.format("-Xms%dm -Xmx%dm -XX:+HeapDumpOnOutOfMemoryError -XX:MaxPermSize=160m -Djava.awt.headless=true", MAX_HEAP_SIZE_IN_MEGA,
        MAX_HEAP_SIZE_IN_MEGA))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/one-xoo-issue-per-line.xml"))
    .build();

  private static File bigProjectBaseDir;

  @BeforeClass
  public static void classSetUp() throws IOException {
    bigProjectBaseDir = createProject(4, 10, 20);
  }

  @Before
  public void before() throws Exception {
    orchestrator.resetData();
  }

  @Test
  public void analyse_big_project() throws Exception {
    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "big-project",
        "sonar.projectName", "Big Project",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "src",
        "sonar.profile", "one-xoo-issue-per-line")
      .setRunnerVersion("2.4")
      .setProjectDir(bigProjectBaseDir);

    orchestrator.executeBuild(runner);

    assertComputationDurationAround(505000);
  }

  private void assertComputationDurationAround(long expectedDuration) throws IOException {
    File report = new File(orchestrator.getServer().getLogs().getParent(), "analysis_reports.log");
    List<String> logsLines = FileUtils.readLines(report, Charsets.UTF_8);
    Long duration = MavenLogs.extractComputationTotalTime(logsLines);

    assertDurationAround(duration, expectedDuration);
  }

  private static File createProject(int dirDepth, int nbDirByLayer, int nbIssuesByFile) throws IOException {
    File rootDir = temp.newFolder();
    File projectProperties = new File(rootDir, "sonar-project.properties");

    StringBuilder moduleListBuilder = new StringBuilder(nbDirByLayer * ("module".length() + 2));

    for (int i = 1; i <= nbDirByLayer; i++) {
      moduleListBuilder.append("module").append(i);
      File moduleDir = new File(rootDir, "module" + i + "/src");
      moduleDir.mkdirs();
      if (i != nbDirByLayer) {
        moduleListBuilder.append(",");
      }

      createProjectFiles(moduleDir, dirDepth - 1, nbDirByLayer, nbIssuesByFile);
    }

    FileUtils.write(projectProperties, "sonar.modules=", true);
    FileUtils.write(projectProperties, moduleListBuilder.toString(), true);
    FileUtils.write(projectProperties, "\n", true);
    FileUtils.write(projectProperties, "sonar.source=src", true);

    return rootDir;
  }

  private static void createProjectFiles(File dir, int depth, int nbFilesByDir, int nbIssuesByFile) throws IOException {
    dir.mkdir();
    for (int i = 1; i <= nbFilesByDir; i++) {
      File xooFile = new File(dir, "file" + i + ".xoo");
      String line = xooFile.getAbsolutePath() + i + "\n";
      FileUtils.write(xooFile, StringUtils.repeat(line, nbIssuesByFile));
      File xooMeasureFile = new File(dir, "file" + i + ".xoo.measures");
      FileUtils.write(xooMeasureFile, "lines:" + nbIssuesByFile);
      if (depth > 1) {
        createProjectFiles(new File(dir, "dir" + i), depth - 1, nbFilesByDir, nbIssuesByFile);
      }
    }
  }
}
