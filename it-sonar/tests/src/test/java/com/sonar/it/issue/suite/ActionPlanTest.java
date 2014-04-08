/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.NewActionPlan;
import org.sonar.wsclient.issue.UpdateActionPlan;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ActionPlanTest extends AbstractIssueTestCase {

  private static final String PROJECT_KEY = "sample";

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);
  }

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncate("action_plans");
    assertThat(adminActionPlanClient().find(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void test_console() {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("action-plans-console",
        "/selenium/issue/action-plans/create_and_delete_action_plan.html",
        "/selenium/issue/action-plans/cannot_create_action_plan_with_date_in_past.html",
        "/selenium/issue/action-plans/cannot_create_action_plan_with_invalid_date.html",
        "/selenium/issue/action-plans/cannot_create_two_action_plans_with_same_name.html",
        // SONAR-3200
        "/selenium/issue/action-plans/close_and_reopen_action_plan.html",
        "/selenium/issue/action-plans/edit_action_plan.html",
        // SONAR-3198
        "/selenium/issue/action-plans/can_create_action_plan_with_date_today.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_create_action_plan() {
    assertThat(adminActionPlanClient().find(PROJECT_KEY)).isEmpty();

    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.name()).isEqualTo("Short term");
    assertThat(actionPlan.description()).isEqualTo("Short term issues");
    assertThat(actionPlan.status()).isEqualTo("OPEN");
    assertThat(actionPlan.project()).isEqualTo(PROJECT_KEY);
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.createdAt()).isNotNull();
    assertThat(actionPlan.updatedAt()).isNotNull();
    assertThat(actionPlan.totalIssues()).isEqualTo(0);
    assertThat(actionPlan.unresolvedIssues()).isEqualTo(0);
  }

  @Test
  public void should_fail_create_action_plan_if_missing_project() {
    try {
      adminActionPlanClient().create(NewActionPlan.create().name("Short term")
        .description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void should_fail_create_action_plan_if_missing_name() {
    try {
      adminActionPlanClient().create(NewActionPlan.create().project(PROJECT_KEY)
        .description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void should_update_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));

    ActionPlan updatedActionPlan = adminActionPlanClient().update(
      UpdateActionPlan.create().key(newActionPlan.key()).name("Long term").description("Long term issues").deadLine(ItUtils.toDate("2114-12-01")));
    assertThat(updatedActionPlan).isNotNull();

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.name()).isEqualTo("Long term");
    assertThat(actionPlan.description()).isEqualTo("Long term issues");
    assertThat(actionPlan.project()).isEqualTo(PROJECT_KEY);
    assertThat(actionPlan.deadLine()).isNotNull();
    assertThat(actionPlan.deadLine()).isNotEqualTo(newActionPlan.deadLine());
    assertThat(actionPlan.updatedAt()).isNotNull();
  }

  @Test
  public void should_fail_update_action_plan_if_missing_name() {
    try {
      adminActionPlanClient().create(
        NewActionPlan.create().project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void should_delete_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));

    adminActionPlanClient().delete(newActionPlan.key());

    List<ActionPlan> results = adminActionPlanClient().find(PROJECT_KEY);
    assertThat(results).isEmpty();
  }

  /**
   * SONAR-4449
   */
  @Test
  public void should_delete_action_plan_also_unplan_linked_issues() {
    // Create action plan
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));

    Issue issue = searchRandomIssue();
    // Link an issue to the action plan
    adminIssueClient().plan(issue.key(), newActionPlan.key());
    // Delete action plan
    adminActionPlanClient().delete(newActionPlan.key());

    // Reload the issue
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.actionPlan()).isNull();
  }

  @Test
  public void should_close_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
    assertThat(firstActionPlan(PROJECT_KEY).status()).isEqualTo("OPEN");

    adminActionPlanClient().close(newActionPlan.key());

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.status()).isEqualTo("CLOSED");
  }

  @Test
  public void should_open_action_plan() {
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));

    adminActionPlanClient().close(newActionPlan.key());
    adminActionPlanClient().open(newActionPlan.key());

    ActionPlan actionPlan = firstActionPlan(PROJECT_KEY);
    assertThat(actionPlan.status()).isEqualTo("OPEN");
  }

  @Test
  public void should_find_action_plans() {
    assertThat(actionPlanClient().find(PROJECT_KEY)).isEmpty();

    adminActionPlanClient().create(NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
    adminActionPlanClient().create(NewActionPlan.create().name("Long term").project(PROJECT_KEY).description("Long term issues"));

    assertThat(actionPlanClient().find(PROJECT_KEY)).hasSize(2);
  }

}
