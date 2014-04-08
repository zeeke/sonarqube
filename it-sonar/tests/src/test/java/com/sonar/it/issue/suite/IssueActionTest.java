/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class IssueActionTest extends AbstractIssueTestCase {

  Issue issue;
  SonarRunner scan;

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);
    issue = searchRandomIssue();
  }

  @Test
  public void no_comments_by_default() throws Exception {
    assertThat(issue.comments()).isEmpty();
  }

  @Test
  public void add_comment() throws Exception {
    IssueComment comment = adminIssueClient().addComment(issue.key(), "this is my *comment*");
    assertThat(comment.key()).isNotNull();
    assertThat(comment.htmlText()).isEqualTo("this is my <em>comment</em>");
    assertThat(comment.login()).isEqualTo("admin");
    assertThat(comment.createdAt()).isNotNull();

    // reload issue
    Issue reloaded = searchIssueByKey(issue.key());

    assertThat(reloaded.comments()).hasSize(1);
    assertThat(reloaded.comments().get(0).key()).isEqualTo(comment.key());
    assertThat(reloaded.comments().get(0).htmlText()).isEqualTo("this is my <em>comment</em>");
    assertThat(reloaded.updateDate().before(issue.creationDate())).isFalse();
  }

  /**
   * SONAR-4450
   */
  @Test
  public void should_reject_blank_comment() throws Exception {
    try {
      adminIssueClient().addComment(issue.key(), "  ");
      fail();
    } catch(HttpException ex) {
      assertThat(ex.status()).isEqualTo(400);
    }

    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.comments()).hasSize(0);
  }

  /**
   * SONAR-4352
   */
  @Test
  public void change_severity() {
    String componentKey = "sample";

    // there are no blocker issues
    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).isEmpty();

    // increase the severity of an issue
    adminIssueClient().setSeverity(issue.key(), "BLOCKER");

    assertThat(searchIssuesBySeverities(componentKey, "BLOCKER")).hasSize(1);

    orchestrator.executeBuild(scan);
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.severity()).isEqualTo("BLOCKER");
    assertThat(reloaded.status()).isEqualTo("OPEN");
    assertThat(reloaded.resolution()).isNull();
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reloaded.creationDate().before(reloaded.updateDate())).isTrue();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void assign() {
    assertThat(issue.assignee()).isNull();
    Issues issues = search(IssueQuery.create().issues(issue.key()));
    assertThat(issues.users()).isEmpty();

    adminIssueClient().assign(issue.key(), "admin");
    assertThat(search(IssueQuery.create().assignees("admin")).list()).hasSize(1);

    orchestrator.executeBuild(scan);
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.assignee()).isEqualTo("admin");
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());

    issues = search(IssueQuery.create().issues(issue.key()));
    assertThat(issues.user("admin")).isNotNull();
    assertThat(issues.user("admin").name()).isEqualTo("Administrator");

    // unassign
    adminIssueClient().assign(issue.key(), null);
    reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.assignee()).isNull();
    assertThat(issueClient().find(IssueQuery.create().assignees("admin")).list()).isEmpty();
  }

  /**
   * SONAR-4287
   */
  @Test
  public void fail_assign_if_assignee_does_not_exist() {
    assertThat(issue.assignee()).isNull();
    try {
      adminIssueClient().assign(issue.key(), "unknown");
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  /**
   * SONAR-4290
   */
  @Test
  public void plan() {
    assertThat(issue.actionPlan()).isNull();

    // Set action plan to issue
    ActionPlan newActionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project("sample")
      .description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();
    adminIssueClient().plan(issue.key(), newActionPlan.key());
    assertThat(search(IssueQuery.create().actionPlans(newActionPlan.key())).list()).hasSize(1);

    orchestrator.executeBuild(scan);
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.actionPlan()).isEqualTo(newActionPlan.key());
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
    ActionPlan actionPlan = search(IssueQuery.create().actionPlans(newActionPlan.key())).actionPlans(reloaded);
    assertThat(actionPlan.name()).isEqualTo(newActionPlan.name());
    assertThat(actionPlan.deadLine()).isEqualTo(newActionPlan.deadLine());
  }

  @Test
  public void fail_plan_if_action_plan_does_not_exist() {
    assertThat(issue.actionPlan()).isNull();
    try {
      adminIssueClient().plan(issue.key(), "unknown");
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void unplan() {
    assertThat(issue.actionPlan()).isNull();

    // Set action plan to issue
    ActionPlan newActionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project("sample")
      .description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();
    adminIssueClient().plan(issue.key(), newActionPlan.key());
    assertThat(search(IssueQuery.create().actionPlans(newActionPlan.key())).list()).hasSize(1);

    // Unplan
    adminIssueClient().plan(issue.key(), null);
    assertThat(search(IssueQuery.create().actionPlans(newActionPlan.key())).list()).hasSize(0);

    orchestrator.executeBuild(scan);
    Issue reloaded = searchIssueByKey(issue.key());
    assertThat(reloaded.actionPlan()).isNull();
    assertThat(reloaded.creationDate()).isEqualTo(issue.creationDate());
  }

  /**
   * SONAR-4315
   */
  @Test
  public void apply_action_from_plugin() {
    // The condition on the action defined by the plugin is that the status must be resolved
    assertThat(adminIssueClient().actions(issue.key())).isEmpty();
    adminIssueClient().doTransition(issue.key(), "resolve");
    assertThat(adminIssueClient().actions(issue.key())).containsOnly("fake");

    adminIssueClient().doAction(issue.key(), "fake");

    // reload issue
    Issue reloaded = searchIssueByKey(issue.key());

    assertThat(reloaded.comments()).hasSize(1);
    assertThat(reloaded.comments().get(0).htmlText()).isEqualTo("New Comment from fake action");
    assertThat(reloaded.attribute("fake")).isEqualTo("fake action");
  }

}
