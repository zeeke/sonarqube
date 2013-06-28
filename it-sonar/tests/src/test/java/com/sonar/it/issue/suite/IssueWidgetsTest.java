/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.NewActionPlan;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueWidgetsTest extends AbstractIssueTestCase {

  private static final String PROJECT_KEY = "com.sonarsource.it.projects.rule:rule-widgets";

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
    Issue critical = searchIssuesByRules(PROJECT_KEY, "pmd:UnconditionalIfStatement").get(0);
    adminIssueClient().assign(critical.key(), "admin");

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("my-unresolved-issues-widget",
        "/selenium/issue/widgets/my-unresolved-issues-widget/should-have-correct-values.html",
        "/selenium/issue/widgets/my-unresolved-issues-widget/should-open-links-on-issue-detail.html"
      ).build());
  }

  /**
   * SONAR-4298
   */
  @Test
  public void test_false_positive_widget() throws Exception {
    Issue critical = searchIssuesByRules(PROJECT_KEY, "pmd:UnconditionalIfStatement").get(0);
    adminIssueClient().doTransition(critical.key(), "falsepositive");

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
    assertThat(issues).hasSize(9);

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
  @Test
  public void test_action_plan_widget() throws Exception {
    // Create a action plan on the project
    ActionPlan actionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));

    // 3 issues will be affected to the action plan : 2 unresolved issues, and 1 resolved

    List<Issue> issues = search(IssueQuery.create()).list();
    assertThat(issues).hasSize(9);

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
   * SONAR-4341
   */
  @Test
  public void test_rules_widgets() throws Exception {
    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("rules-widget",
        "/selenium/issue/widgets/rules/should-have-correct-values.html",
        "/selenium/issue/widgets/rules/should-open-issues-by-severity.html",
        "/selenium/issue/widgets/rules/should-open-issues-count.html",
        "/selenium/issue/widgets/rules/should-open-weighted-issues.html"
      ).build());
  }

  /**
   * SONAR-3081
   * SONAR-4341
   */
  @Test
  public void test_rules_widgets_on_differential_view() throws Exception {
    // let's exclude 1 file to have cleared issues
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("rule/rule-widgets"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.exclusions", "**/FewViolations.java")
      .setProfile("sonar-way-2.7"));

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("rules-widget-differential-view-cleared-issues",
        "/selenium/issue/widgets/rules/diff-view-should-show-cleared-issues-count.html"
      ).build());

    // And let's run again to have new issues
    analyzeProject();

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("rules-widget-differential-view-new-issues",
        "/selenium/issue/widgets/rules/diff-view-should-show-new-issues-count.html",
        "/selenium/issue/widgets/rules/diff-view-should-open-new-issues-on-drilldown.html"
      ).build());
  }

  private void analyzeProject(){
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("rule/rule-widgets"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProfile("sonar-way-2.7"));
  }

}
