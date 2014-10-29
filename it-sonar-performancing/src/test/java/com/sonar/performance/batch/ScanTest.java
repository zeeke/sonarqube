/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.performance.PerfTestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.issue.BulkChangeQuery;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class ScanTest extends PerfTestCase {

  private static final int NB_LINES_PER_FILE_BIG_PROJECT = 1000;

  private static final int NB_FILES_BIG_PROJECT = 100;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(MavenLocation.create("com.sonarsource.xoo", "sonar-xoo-plugin", "1.0-SNAPSHOT"))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/one-xoo-issue-per-line.xml"))
    .build();

  @BeforeClass
  public static void setUp() throws IOException {
    // Execute this query in order for next analysis to not freeze (FIXME why does it freeze?)
    orchestrator.resetData();

    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newSonarRunner("-Xmx512m -server", "sonar.profile", "one-xoo-issue-per-line"));
  }

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void scan_xoo_project() {
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true",
      "sonar.scm.disabled", "true"
      );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;
    assertDurationAround(duration, 9700L);
  }

  @Test
  public void scan_100000_issues_tracking() throws InvalidPropertiesFormatException, IOException {
    File projectBaseDir = createBigXooProject(NB_FILES_BIG_PROJECT, NB_LINES_PER_FILE_BIG_PROJECT);
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true",
      "sonar.scm.disabled", "true",
      "sonar.projectKey", "foo",
      "sonar.projectName", "Foo",
      "sonar.projectVersion", "1.0",
      "sonar.sources", "src"
      ).setProjectDir(projectBaseDir);
    orchestrator.executeBuild(runner);
    Properties prof = readProfiling(projectBaseDir, "foo");
    assertDurationAround(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 1645L);

    // Second run
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    assertDurationAround(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 47000L);
    assertDurationAround(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 6900L);
  }

  @Test
  public void scan_500_issues_with_changelog_tracking() throws InvalidPropertiesFormatException, IOException {
    int nbFiles = 10;
    int nbIssuesPerFile = 50;
    File projectBaseDir = createBigXooProject(nbFiles, nbIssuesPerFile);
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true",
      "sonar.scm.disabled", "true",
      "sonar.projectKey", "foo",
      "sonar.projectName", "Foo",
      "sonar.projectVersion", "1.0",
      "sonar.sources", "src"
      ).setProjectDir(projectBaseDir);
    orchestrator.executeBuild(runner);
    Properties prof = readProfiling(projectBaseDir, "foo");

    Set<String> issueKeys = new HashSet<String>();
    int count = nbFiles * nbIssuesPerFile;
    int pageSize = 500;
    for (int i = 1; i <= count / pageSize; i++) {
      Issues issues = orchestrator.getServer().adminWsClient().issueClient().find(IssueQuery.create().componentRoots("foo").pageSize(pageSize).pageIndex(i));
      for (Issue issue : issues.list()) {
        issueKeys.add(issue.key());
      }
    }
    assertThat(issueKeys).hasSize(count);

    long start = System.currentTimeMillis();
    // Create some changelog per issue
    for (int i = 1; i <= 2; i++) {
      orchestrator.getServer().adminWsClient().issueClient().bulkChange(BulkChangeQuery.create()
        .issues(issueKeys.toArray(new String[pageSize]))
        .actions("assign", "set_severity", "do_transition")
        .actionParameter("assign", "assignee", "admin")
        .actionParameter("do_transition", "transition", "confirm")
        .actionParameter("set_severity", "severity", "MINOR"));
      orchestrator.getServer().adminWsClient().issueClient().bulkChange(BulkChangeQuery.create()
        .issues(issueKeys.toArray(new String[pageSize]))
        .actions("assign", "set_severity", "do_transition")
        .actionParameter("assign", "assignee", "")
        .actionParameter("do_transition", "transition", "unconfirm")
        .actionParameter("set_severity", "severity", "BLOCKER"));
    }
    System.out.println("Creating changelog took: " + (System.currentTimeMillis() - start) + " ms");

    // Second run
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    assertDurationAround(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 24000L);
  }

  private Properties readProfiling(File baseDir, String moduleKey) throws FileNotFoundException, IOException, InvalidPropertiesFormatException {
    File profiling = new File(baseDir, ".sonar/profiling/" + moduleKey + "-profiler.xml");
    Properties props = new Properties();
    FileInputStream in = new FileInputStream(profiling);
    try {
      props.loadFromXML(in);
    } finally {
      in.close();
    }
    return props;
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
    long firstDuration = System.currentTimeMillis() - start;
    System.out.println("First preview analysis: " + firstDuration + "ms");

    // caches are warmed
    start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long secondDuration = System.currentTimeMillis() - start;
    System.out.println("Second preview analysis: " + secondDuration + "ms");

    assertDurationAround(secondDuration, 9800L);
  }

  @Test
  public void should_not_fail_with_limited_xmx_memory_and_no_coverage_per_test() {
    orchestrator.executeBuild(
      newSonarRunner("-Xmx80m -server -XX:-HeapDumpOnOutOfMemoryError")
      );
  }

  @Test
  public void computeSyntaxHighlightingOnBigFiles() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    int nbFiles = 100;
    int chunkSize = 100000;
    for (int nb = 1; nb <= nbFiles; nb++) {
      File xooFile = new File(srcDir, "sample" + nb + ".xoo");
      File xoohighlightingFile = new File(srcDir, "sample" + nb + ".xoo.highlighting");
      FileUtils.write(xooFile, "Sample xoo\ncontent");
      StringBuilder sb = new StringBuilder(16 * chunkSize);
      for (int i = 0; i < chunkSize; i++) {
        sb.append(i).append(":").append(i + 1).append(":s\n");
      }
      FileUtils.write(xoohighlightingFile, sb.toString());
    }

    SonarRunner runner = SonarRunner.create()
      .setProperties(
        "sonar.projectKey", "highlighting",
        "sonar.projectName", "highlighting",
        "sonar.projectVersion", "1.0",
        "sonar.sources", "src",
        "sonar.showProfiling", "true")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx512m -server -XX:MaxPermSize=64m")
      .setRunnerVersion("2.4")
      .setProjectDir(baseDir);

    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;
    assertDurationAround(duration, 43000L);
  }

  private static SonarRunner newSonarRunner(String sonarRunnerOpts, String... props) {
    return SonarRunner.create()
      .setProperties(props)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", sonarRunnerOpts)
      .setRunnerVersion("2.3")
      .setProjectDir(FileLocation.of("projects/xoo-sample").getFile());
  }

  private static File createBigXooProject(int nbFiles, int nbIssuesPerFile) throws IOException {
    File baseDir = temp.newFolder();

    File src = new File(baseDir, "src");
    for (int i = 1; i <= nbFiles; i++) {
      File xooFile = new File(src, "file" + i + ".xoo");
      FileUtils.write(xooFile, "Sample content");
      File xooMeasureFile = new File(src, "file" + i + ".xoo.measures");
      FileUtils.write(xooMeasureFile, "lines:" + nbIssuesPerFile);
    }

    return baseDir;
  }

}
