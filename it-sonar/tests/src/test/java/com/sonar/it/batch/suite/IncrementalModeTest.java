/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class IncrementalModeTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File dryRunCacheLocation;

  @Before
  public void deleteData() {
    orchestrator.resetData();
    dryRunCacheLocation = new File(new File(orchestrator.getServer().getHome(), "temp"), "dryRun");
    FileUtils.deleteQuietly(dryRunCacheLocation);
  }

  // SONAR-3677
  @Test
  public void test_incremental_mode() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
    // First regular analysis
    String profileName = "one-issue-per-line";
    SonarRunner runner = configureRunner("batch/incremental/xoo-sample-v1")
      .setProfile(profileName);
    orchestrator.executeBuild(runner);

    Issues issues = orchestrator.getServer().adminWsClient().issueClient().find(IssueQuery.create().components("incremental"));
    assertThat(issues.size()).isEqualTo(13 + 20);

    runner = configureRunner("batch/incremental/xoo-sample-v1",
      "sonar.analysis.mode", "incremental")
      .setProfile(profileName);
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(0);

    runner = configureRunner("batch/incremental/xoo-sample-v2",
      "sonar.analysis.mode", "incremental")
      .setProfile(profileName);
    result = orchestrator.executeBuild(runner);
    // 2 new lines in Sample2 -> 20 old issues + 2 new issues
    // New Sample3 -> 5 new issues
    // Sample1 unmodified
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(20 + 2 + 5);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(2 + 5);
  }

  // SONAR-4985
  // SONAR-3718
  @Test
  public void test_incremental_mode_on_branch() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
    // First regular analysis
    String profileName = "one-issue-per-line";
    SonarRunner runner = configureRunner("batch/incremental/xoo-sample-v1")
      .setProperty("sonar.branch", "my-branch/1.0")
      .setProfile(profileName);
    orchestrator.executeBuild(runner);

    Issues issues = orchestrator.getServer().adminWsClient().issueClient().find(IssueQuery.create().components("incremental:my-branch/1.0"));
    assertThat(issues.size()).isEqualTo(13 + 20);

    runner = configureRunner("batch/incremental/xoo-sample-v1",
      "sonar.analysis.mode", "incremental")
      .setProperty("sonar.branch", "my-branch/1.0")
      .setProfile(profileName);
    BuildResult result = orchestrator.executeBuild(runner);
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(0);

    runner = configureRunner("batch/incremental/xoo-sample-v2",
      "sonar.analysis.mode", "incremental")
      .setProperty("sonar.branch", "my-branch/1.0")
      .setProfile(profileName);
    result = orchestrator.executeBuild(runner);
    // 2 new lines in Sample2 -> 20 old issues + 2 new issues
    // New Sample3 -> 5 new issues
    // Sample1 unmodified
    assertThat(ItUtils.countIssuesInJsonReport(result, false)).isEqualTo(20 + 2 + 5);
    assertThat(ItUtils.countIssuesInJsonReport(result, true)).isEqualTo(2 + 5);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      .setProperties(props);
    return runner;
  }

}
