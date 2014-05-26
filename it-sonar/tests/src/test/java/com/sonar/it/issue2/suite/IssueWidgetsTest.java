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
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.NewActionPlan;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueWidgetsTest extends AbstractIssueTestCase2 {

  private static final String PROJECT_KEY = "sample";

  @Before
  public void before() throws Exception {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
    analyzeProject();
  }

  /**
   * SONAR-4292
   */
  @Test
  public void test_unresolved_issues_per_assignee_widget() throws Exception {
    adminIssueClient().assign(searchRandomIssue().key(), "admin");

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("unresolved-issues-per-assignee-widget",
        "/selenium/issue/widgets/unresolved-issues-per-assignee-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/unresolved-issues-per-assignee-widget/should-open-links-on-issues.html"
      ).build());
  }

  /**
   * SONAR-4293
   */
  @Test
  public void test_my_unresolved_issues_widget() throws Exception {
    adminIssueClient().assign(searchRandomIssue().key(), "admin");

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("my-unresolved-issues-widget",
        "/selenium/issue/widgets/my-unresolved-issues-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/my-unresolved-issues-widget/should-open-links-on-issue-detail.html"
      ).build());
  }

  /**
   * SONAR-4620
   */
  @Test
  public void test_open_issue_filter_from_issues_widget() throws Exception {
    // By default page size is 5 so assign 6 issues to have pagination
    List<Issue> issues = search(IssueQuery.create()).list();
    int count = 0;
    for (Issue issue : issues) {
      adminIssueClient().assign(issue.key(), "admin");
      if (++count >= 6) {
        break;
      }
    }

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("open_issue_filter_from_issues_widget",
        "/selenium/issue/widgets/open_issue_filter_from_issues_widget/should-open-issue-filter.html"
      ).build());
  }

  /**
   * SONAR-4298
   */
  @Test
  public void test_false_positive_widget() throws Exception {
    adminIssueClient().doTransition(searchRandomIssue().key(), "falsepositive");

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("false-positive-widget",
        "/selenium/issue/widgets/false-positive-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/false-positive-widget/should-open-links-on-issue-detail.html"
      ).build());
  }

  /**
   * SONAR-4333
   */
  @Test
  public void test_unresolved_issue_statuses_widget() throws Exception {
    List<Issue> issues = searchIssuesByComponent(PROJECT_KEY);
    assertThat(issues).hasSize(13);

    // 1 is a false-positive, 2 are confirmed, 1 is reopened
    adminIssueClient().doTransition(issues.get(0).key(), "falsepositive");
    adminIssueClient().doTransition(issues.get(1).key(), "confirm");
    adminIssueClient().doTransition(issues.get(2).key(), "confirm");
    adminIssueClient().doTransition(issues.get(3).key(), "resolve");
    adminIssueClient().doTransition(issues.get(3).key(), "reopen");

    // Re analyze the project to have measures on metrics
    analyzeProject();

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("unresolved-issue-statuses-widget",
        "/selenium/issue/widgets/unresolved-issue-statuses-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/unresolved-issue-statuses-widget/should-open-links-on-issues.html"
      ).build());
  }

  /**
   * SONAR-4296
   */
  @Ignore("Need /issues/search to manage 'actionPlans' and 'resolved' params")
  @Test
  public void test_action_plan_widget() throws Exception {
    // Create a action plan on the project
    ActionPlan actionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));

    // 3 issues will be affected to the action plan : 2 unresolved issues, and 1 resolved

    List<Issue> issues = search(IssueQuery.create()).list();
    assertThat(issues).hasSize(13);

    adminIssueClient().plan(issues.get(0).key(), actionPlan.key());
    adminIssueClient().plan(issues.get(1).key(), actionPlan.key());

    adminIssueClient().doTransition(issues.get(2).key(), "resolve");
    adminIssueClient().plan(issues.get(2).key(), actionPlan.key());

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("action-plan-widget",
        "/selenium/issue/widgets/action-plan-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/action-plan-widget/should-open-links-on-manage-action-plan.html",
        "/selenium/issue/widgets/action-plan-widget/should-hide-resolved-issues.html",
        "/selenium/issue/widgets/action-plan-widget/should-open-resolved-issues-progressbar-link.html",
        "/selenium/issue/widgets/action-plan-widget/should-open-resolved-issues-text-link.html",
        "/selenium/issue/widgets/action-plan-widget/should-open-total-issues-text-link.html",
        "/selenium/issue/widgets/action-plan-widget/should-open-unsolved-issues-progressbar-link.html"
      ).build());
  }

  /**
   * SONAR-3557
   */
  @Test
  @Ignore("Need to find a way to retrieve filter id that have just been created")
  public void test_issue_filter_widget() throws Exception {
    orchestrator.executeSelenese(Selenese.builder()
      .setHtmlTestsInClasspath("issue-filter-widget",
        "/selenium/issue/widgets/issue-filter-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/issue-filter-widget/should-open-links-on-issues.html"
      ).build());
  }

  private void analyzeProject() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));
  }

}
