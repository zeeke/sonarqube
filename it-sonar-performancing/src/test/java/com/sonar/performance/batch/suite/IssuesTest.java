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
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.issue.BulkChangeQuery;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class IssuesTest extends PerfTestCase {

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
  public void scan_xoo_project() {
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true"
      );
    BuildResult result = orchestrator.executeBuild(runner);
    assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 7800L);
  }

  @Test
  public void scan_100_files_1000_issues_tracking() throws IOException {
    File projectBaseDir = createBigXooProject(100, 1000);
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true",
      "sonar.projectKey", "foo",
      "sonar.projectName", "100 Files 1000 Issues",
      "sonar.projectVersion", "1.0",
      "sonar.sources", "src"
      ).setProjectDir(projectBaseDir);
    orchestrator.executeBuild(runner);
    Properties prof = readProfiling(projectBaseDir, "foo");
    assertDurationLessThan(collector, Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 2097L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssuePersister")), 31000L);

    // Second run
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    assertDurationAround(collector, Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10300L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 3788L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssuePersister")), 424L);
  }

  @Test
  public void scan_1000_files_no_issues_tracking() throws IOException {
    File projectBaseDir = createBigXooProject(1000, 1000);
    // Use empty profile
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.showProfiling", "true",
      "sonar.projectKey", "foo",
      "sonar.projectName", "1000 Files no issue",
      "sonar.projectVersion", "1.0",
      "sonar.sources", "src"
      ).setProjectDir(projectBaseDir);
    orchestrator.executeBuild(runner);
    Properties prof = readProfiling(projectBaseDir, "foo");
    assertDurationLessThan(collector, Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10L);
    assertDurationLessThan(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 150L);
    assertDurationLessThan(collector, Long.valueOf(prof.getProperty("IssuePersister")), 10L);
  }

  @Test
  public void scan_1_files_100000_issues_tracking() throws IOException {
    File projectBaseDir = createBigXooProject(1, 100000);
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true",
      "sonar.projectKey", "foo",
      "sonar.projectName", "1 File 100000 Issues",
      "sonar.projectVersion", "1.0",
      "sonar.sources", "src"
      ).setProjectDir(projectBaseDir);
    orchestrator.executeBuild(runner);
    Properties prof = readProfiling(projectBaseDir, "foo");
    assertDurationLessThan(collector, Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 12000L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssuePersister")), 30000L);

    // Second run
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    assertDurationAround(collector, Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10700L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 17000L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssuePersister")), 427L);
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
      "sonar.projectKey", "foo",
      "sonar.projectName", "500 Issues with changelog",
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
    for (int i = 1; i <= 5; i++) {
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
    assertDurationAround(collector, Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 1916L);
    assertDurationAround(collector, Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 1228L);
    assertDurationLessThan(collector, Long.valueOf(prof.getProperty("IssuePersister")), 200L);
  }

  private static File createBigXooProject(int nbFiles, int nbIssuesPerFile) throws IOException {
    File baseDir = temp.newFolder();

    File src = new File(baseDir, "src");
    for (int i = 1; i <= nbFiles; i++) {
      File xooFile = new File(src, "file" + i + ".xoo");
      FileUtils.write(xooFile, StringUtils.repeat("a\n", nbIssuesPerFile));
      File xooMeasureFile = new File(src, "file" + i + ".xoo.measures");
      FileUtils.write(xooMeasureFile, "lines:" + nbIssuesPerFile);
    }

    return baseDir;
  }

}
