/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.issue.NewIssue;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueTest extends AbstractIssueTestCase2 {

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void test_common_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTest/with-many-rules.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("with-many-rules");
    orchestrator.executeBuild(scan);

    String projectKey = "com.sonarsource.it.samples:multi-modules-sample";
    assertThat(searchIssuesByProject(projectKey)).hasSize(62);

    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(projectKey, "violations", "info_violations", "minor_violations", "major_violations",
        "blocker_violations", "critical_violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(62);
    assertThat(project.getMeasureIntValue("info_violations")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("minor_violations")).isEqualTo(52);
    assertThat(project.getMeasureIntValue("major_violations")).isEqualTo(4);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("critical_violations")).isEqualTo(4);
  }

  /**
   * SONAR-4330
   */
  @Test
  public void test_resolution_and_status_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    String projectKey = "sample";
    List<Issue> issues = searchIssuesByProject(projectKey);
    assertThat(issues).hasSize(13);

    // 1 is a false-positive, 1 is confirmed, 1 is reopened, and the remaining ones stays open
    adminIssueClient().doTransition(issues.get(0).key(), "falsepositive");
    adminIssueClient().doTransition(issues.get(1).key(), "confirm");
    adminIssueClient().doTransition(issues.get(2).key(), "resolve");
    adminIssueClient().doTransition(issues.get(2).key(), "reopen");

    // Re analyze the project to compute measures
    orchestrator.executeBuild(scan);

    Resource project = orchestrator.getServer().getWsClient().find(
      ResourceQuery.createForMetrics(projectKey, "false_positive_issues", "open_issues", "reopened_issues", "confirmed_issues"));
    assertThat(project.getMeasureIntValue("false_positive_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("open_issues")).isEqualTo(10);
    assertThat(project.getMeasureIntValue("reopened_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("confirmed_issues")).isEqualTo(1);
  }

  @Test
  public void get_no_issue_on_empty_profile() {
    // no active rules
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("empty");
    orchestrator.executeBuild(scan);

    assertThat(searchIssuesByProject("sample")).isEmpty();

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "violations", "blocker_violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(0);
  }

  @Test
  public void close_no_more_existing_issue() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    String projectKey = "sample";
    List<Issue> issues = searchIssuesByProject(projectKey);
    assertThat(issues).hasSize(13);
    for (Issue issue : issues) {
      assertThat(issue.status()).isEqualTo("OPEN");
    }

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, "violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(13);

    // Empty profile -> no issue
    orchestrator.executeBuild(scan.setProfile("empty"));

    issues = searchIssuesByProject(projectKey);
    assertThat(issues).hasSize(13);
    for (Issue issue : issues) {
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, "violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(0);
  }

  /**
   * SONAR-3746
   */
  @Test
  public void compute_issues_metrics_on_test_files() {
    String projectKey = "sample-with-tests";
    String testKey = "sample-with-tests:src/test/xoo/sample/SampleTest.xoo";

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample-with-tests"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    Sonar wsClient = orchestrator.getServer().getAdminWsClient();

    // Store current number of issues
    Resource project = wsClient.find(ResourceQuery.createForMetrics(projectKey, "violations"));
    int issues = project.getMeasureIntValue("violations");

    // Create the manual rule
    createManualRule();

    // Create a issue on the test source file
    adminIssueClient().create(NewIssue.create().component(testKey)
      .severity("MAJOR")
      .rule("manual:invalidclassname").line(8)
      .message("The name 'SampleTest' is too generic"));

    // Re-analyse the project
    orchestrator.executeBuild(scan);

    // And check that the number of issues metrics have changed
    project = wsClient.find(ResourceQuery.createForMetrics(projectKey, "violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(issues + 1);
  }


  /**
   * See SONAR-4785
   */
  @Test
  public void get_rule_name_if_issue_has_no_message() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTest/with-custom-message.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("with-custom-message");
    orchestrator.executeBuild(scan.setProperties("sonar.customMessage.message", ""));
    Issue issue = issueClient().find(IssueQuery.create()).list().get(0);
    assertThat(issue.message()).isEqualTo("Issue With Custom Message");

    orchestrator.executeBuild(scan.setProperties("sonar.customMessage.message", null));
    issue = issueClient().find(IssueQuery.create()).list().get(0);
    assertThat(issue.message()).isEqualTo("Issue With Custom Message");
  }


  @Test
  public void plugin_can_override_profile_severity() throws Exception {
    // The rule "OneBlockerIssuePerFile" is enabled with severity "INFO"
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue2/IssueTest/override-profile-severity.xml"));

    // But it's hardcoded "blocker" when plugin generates the issue
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).setProfile("override-profile-severity");
    orchestrator.executeBuild(scan);

    Issues issues = search(IssueQuery.create().rules("xoo:OneBlockerIssuePerFile"));
    assertThat(issues.size()).isGreaterThan(0);
    for (Issue issue : issues.list()) {
      assertThat(issue.severity()).isEqualTo("BLOCKER");
    }
  }
}
