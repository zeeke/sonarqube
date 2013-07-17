/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.NewIssue;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueTest extends AbstractIssueTestCase {

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @AfterClass
  public static void purgeManualRules(){
    deleteManualRules();
  }

  @Test
  public void test_common_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/multi-modules-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("issues")
      // Multi module project have to use sonar-runner 2.2.2 to not fail
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(scan);

    String componentKey = "multi-modules-sample";
    assertThat(searchIssuesByComponent(componentKey)).hasSize(20);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(componentKey, "violations", "info_violations", "minor_violations", "major_violations",
      "blocker_violations", "critical_violations", "violations_density"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(20);
    assertThat(project.getMeasureIntValue("info_violations")).isEqualTo(8);
    assertThat(project.getMeasureIntValue("minor_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("major_violations")).isEqualTo(12);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("critical_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("violations_density")).isEqualTo(7);
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

    String componentKey = "sample";
    List<Issue> issues = searchIssuesByComponent(componentKey);
    assertThat(issues).hasSize(13);

    // 1 is a false-positive, 1 is confirmed, 1 is reopened, and the remaining ones stays open
    adminIssueClient().doTransition(issues.get(0).key(), "falsepositive");
    adminIssueClient().doTransition(issues.get(1).key(), "confirm");
    adminIssueClient().doTransition(issues.get(2).key(), "resolve");
    adminIssueClient().doTransition(issues.get(2).key(), "reopen");

    // Re analyze the project to compute measures
    orchestrator.executeBuild(scan);

    Resource project = orchestrator.getServer().getWsClient().find(
      ResourceQuery.createForMetrics(componentKey, "false_positive_issues", "open_issues", "reopened_issues", "confirmed_issues"));
    assertThat(project.getMeasureIntValue("false_positive_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("open_issues")).isEqualTo(10);
    assertThat(project.getMeasureIntValue("reopened_issues")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("confirmed_issues")).isEqualTo(1);
  }

  @Test
  public void should_get_no_issue_on_empty_profile() {
    // no active rules
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperties("sonar.cpd.skip", "true")
        .setProfile("empty");
    orchestrator.executeBuild(scan);

    assertThat(searchIssuesByComponent("sample")).isEmpty();

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "violations", "blocker_violations", "violations_density"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("violations_density")).isEqualTo(100);
  }

  @Test
  public void should_close_no_more_existing_issue() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperties("sonar.cpd.skip", "true")
        .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    String projectKey = "sample";
    List<Issue> issues = searchIssuesByComponent(projectKey);
    assertThat(issues).hasSize(13);
    for (Issue issue : issues) {
      assertThat(issue.status()).isEqualTo("OPEN");
    }

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, "violations"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(13);

    // Empty profile -> no issue
    orchestrator.executeBuild(scan.setProfile("empty"));

    issues = searchIssuesByComponent(projectKey);
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
  public void should_compute_issues_metrics_on_test_files() {
    String projectKey = "sample-with-tests";
    String testKey = "sample-with-tests:sample/SampleTest.xoo";

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
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("create-manual-rule",
      "/selenium/issue/manual-issue/create-manual-rule.html"
    ).build());

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
   * See SONAR-582 - issues not attached to a line of code
   */
  @Test
  public void should_get_issues_even_if_no_issue_on_line_of_code() {
    // all the detected issues are not attached to a line
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/backup-for-file-global-issues.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("issue/file-global-issues"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("global-issues");
    orchestrator.executeBuild(scan);

    List<Issue> issues = searchIssuesByComponent("file-global-issues:sample.Sample");
    assertThat(issues.size()).isGreaterThan(0);
    for (Issue issue : issues) {
      assertThat(issue.line()).describedAs("issue with line: " + issue.ruleKey()).isNull();
    }

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("file-global-issues",
      "/selenium/issue/file-global-issues.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See SONAR-684
   */
  @Test
  public void should_encode_issue_messages() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("issue/encoded-issue-message"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("sonar-way-2.7");
    orchestrator.executeBuild(scan);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("encoded-issue-message",
      "/selenium/issue/encoded-issue-message.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See SONAR-413
   */
  @Test
  public void should_not_have_issues_on_tests() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("issue/no-issue-on-tests"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("issues");
    orchestrator.executeBuild(scan);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("no-issue-on-tests",
      "/selenium/issue/no-issue-on-tests.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4210
   */
  @Test
  public void test_issue_drilldown() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/multi-modules-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("issues")
      // Multi module project have to use sonar-runner 2.2.2 to not fail
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(scan);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("issues-drilldown",
      "/selenium/issue/issues-drilldown/unselect-module-filter.html",
      "/selenium/issue/issues-drilldown/unselect-rule-filter.html",
      "/selenium/issue/issues-drilldown/guess-rule-severity.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4359
   * SONAR-4301
   */
  @Test
  public void test_code_viewer() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("issues");
    orchestrator.executeBuild(scan);

    // Resolve an issue
    Issue issue = search(IssueQuery.create().componentRoots("sample:sample.Sample").rules("pmd:UnusedLocalVariable")).list().get(0);
    adminIssueClient().doTransition(issue.key(), "resolve");

    // Mark an issue as false positive
    issue = search(IssueQuery.create().componentRoots("sample:sample.Sample")
      .rules("checkstyle:com.puppycrawl.tools.checkstyle.checks.whitespace.FileTabCharacterCheck")).list().get(0);
    adminIssueClient().doTransition(issue.key(), "falsepositive");

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("issues-code-viewer",
      "/selenium/issue/issues-code-viewer/display-only-unresolved-issues.html",
      "/selenium/issue/issues-code-viewer/display-only-false-positives.html"
    ).build());
  }

  /**
   * SONAR-4303
   */
  @Test
  public void test_issue_detail() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("issues");
    orchestrator.executeBuild(scan);

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("issue-detail",
        "/selenium/issue/issue-detail/test-issue-detail.html",
        "/selenium/issue/issue-detail/should-open-link-on-component.html",
        "/selenium/issue/issue-detail/should-open-rule-detail.html",
        "/selenium/issue/issue-detail/should-open-link-on-permalink-issue.html",
        // SONAR-4284
        "/selenium/issue/issue-detail/should-open-changelog.html",
        "/selenium/issue/issue-detail/should-display-actions-when-logged.html"
    ).build());
  }

  @Test
  public void test_file_with_thousands_issues() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir("issue/file-with-thousands-issues"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(runner);

    IssueQuery query = IssueQuery.create().components("file-with-thousands-issues:long_file.xoo");
    assertThat(search(query).list().size()).isGreaterThan(3000);
  }

}
