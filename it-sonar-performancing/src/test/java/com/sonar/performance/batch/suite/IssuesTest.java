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
import com.sonar.performance.PerfRule;
import com.sonar.performance.PerfTestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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
  public PerfRule perfRule = new PerfRule(4) {
    @Override
    protected void beforeEachRun() {
      orchestrator.resetData();
    }
  };

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = BatchPerfTestSuite.ORCHESTRATOR;

  private static SonarRunner runnerForBigProjectWithManyChangelog;

  @BeforeClass
  public static void prepare() throws IOException {
    runnerForBigProjectWithManyChangelog = prepareBigProjectWithManyIssuesAndManyChangelog();
  }

  @Test
  public void scan_xoo_project() {
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.showProfiling", "true"
      );
    BuildResult result = orchestrator.executeBuild(runner);
    assertDurationAround(MavenLogs.extractTotalTime(result.getLogs()), 8200L);
  }

  @Test
  public void scan_100_files_1000_issues_tracking() throws IOException {
    File projectBaseDir = createBigXooProject(100, 1000);
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.showProfiling", "true",
      "sonar.projectKey", "foo",
      "sonar.projectName", "100 Files 1000 Issues",
      "sonar.projectVersion", "1.0",
      "sonar.sources", "src"
      ).setProjectDir(projectBaseDir);
    // First run with no issues
    orchestrator.executeBuild(runner);
    Properties prof = readProfiling(projectBaseDir, "foo");
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10L);
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 200L);
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("IssuePersister")), 10L);

    // Second run with new issues
    runner.setProperty("sonar.profile", "one-xoo-issue-per-line");
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 1500L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssuePersister")), 28000L);

    // Third run with issue tracking
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 8800L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 3794L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssuePersister")), 424L);
  }

  @Test
  public void scan_1_files_10000_issues_tracking() throws IOException {
    File projectBaseDir = createBigXooProject(1, 10000);
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
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 10L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 2010L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssuePersister")), 3900L);

    // Second run
    orchestrator.executeBuild(runner);
    prof = readProfiling(projectBaseDir, "foo");
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 2290L);
    perfRule.assertDurationAround(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 1850L);
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("IssuePersister")), 100L);
  }

  @Test
  public void scan_500_issues_with_changelog_tracking() throws InvalidPropertiesFormatException, IOException {
    // Second run
    orchestrator.executeBuild(runnerForBigProjectWithManyChangelog);
    Properties prof = readProfiling(runnerForBigProjectWithManyChangelog.getProjectDir(), "foo");
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("InitialOpenIssuesSensor")), 100L);
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("IssueTrackingDecorator")), 300L);
    perfRule.assertDurationLessThan(Long.valueOf(prof.getProperty("IssuePersister")), 460L);
  }

  private static SonarRunner prepareBigProjectWithManyIssuesAndManyChangelog() throws IOException {
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

    Set<String> issueKeys = new HashSet<String>();
    int count = nbFiles * nbIssuesPerFile;
    int pageSize = 500;
    for (int i = 1; i <= (count / pageSize) + 1; i++) {
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
        .issues(issueKeys.toArray(new String[0]))
        .actions("assign", "set_severity", "do_transition")
        .actionParameter("assign", "assignee", "admin")
        .actionParameter("do_transition", "transition", "confirm")
        .actionParameter("set_severity", "severity", "MINOR"));
      orchestrator.getServer().adminWsClient().issueClient().bulkChange(BulkChangeQuery.create()
        .issues(issueKeys.toArray(new String[0]))
        .actions("assign", "set_severity", "do_transition")
        .actionParameter("assign", "assignee", "")
        .actionParameter("do_transition", "transition", "unconfirm")
        .actionParameter("set_severity", "severity", "BLOCKER"));
    }
    System.out.println("Creating changelog took: " + (System.currentTimeMillis() - start) + " ms");
    return runner;
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
