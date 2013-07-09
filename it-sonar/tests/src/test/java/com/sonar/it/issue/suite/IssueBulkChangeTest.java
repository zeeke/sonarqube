/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.issue.suite;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.issue.*;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-4421
 */
public class IssueBulkChangeTest extends AbstractIssueTestCase {

  private final static String PROJECT_KEY = "sample";

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void should_change_severity() {
    analyzeSampleProjectWillSmallIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);

    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("set_severity")
        .actionParameter("set_severity", "severity", newSeverity)
    );
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.severity()).isEqualTo(newSeverity);
    }
  }

  @Test
  public void should_do_transition() {
    analyzeSampleProjectWillSmallIssues();
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);
    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("do_transition")
        .actionParameter("do_transition", "transition", "confirm")
    );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.status()).isEqualTo("CONFIRMED");
    }
  }

  @Test
  public void should_assign() {
    analyzeSampleProjectWillSmallIssues();
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);
    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("assign")
        .actionParameter("assign", "assignee", "admin")
    );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.assignee()).isEqualTo("admin");
    }
  }

  @Test
  public void should_plan() {
    analyzeSampleProjectWillSmallIssues();

    // Create action plan
    ActionPlan newActionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project(PROJECT_KEY).description("Short term issues").deadLine(toDate("2113-01-31")));

    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);
    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("plan")
        .actionParameter("plan", "plan", newActionPlan.key())
    );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.actionPlan()).isEqualTo(newActionPlan.key());
    }
  }

  @Test
  public void should_add_comment() {
    analyzeSampleProjectWillSmallIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);

    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("set_severity", "comment")
        .actionParameter("set_severity", "severity", newSeverity)
        .actionParameter("comment", "comment", "this is my *comment*")
    );
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.comments()).hasSize(1);
      assertThat(issue.comments().get(0).htmlText()).isEqualTo("this is my <em>comment</em>");
    }
  }

  @Test
  public void should_apply_bulk_change_on_many_actions() {
    analyzeSampleProjectWillSmallIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);
    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("do_transition", "assign", "set_severity", "comment")
        .actionParameter("do_transition", "transition", "confirm")
        .actionParameter("assign", "assignee", "admin")
        .actionParameter("set_severity", "severity", newSeverity)
        .actionParameter("comment", "comment", "this is my *comment*")
    );

    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.status()).isEqualTo("CONFIRMED");
      assertThat(issue.assignee()).isEqualTo("admin");
      assertThat(issue.severity()).isEqualTo(newSeverity);
      assertThat(issue.comments()).hasSize(1);
      assertThat(issue.comments().get(0).htmlText()).isEqualTo("this is my <em>comment</em>");
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_not_logged() {
    analyzeSampleProjectWillSmallIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);

    BulkChangeQuery query = (BulkChangeQuery.create().issues(issueKeys).actions("set_severity").actionParameter("set_severity", "severity", newSeverity));
    try {
      issueClient().bulkChange(query);
    } catch (Exception e) {
      verifyHttpException(e, 401);
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_no_change_to_do() {
    analyzeSampleProjectWillSmallIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);

    // Apply the bulk change a first time
    BulkChangeQuery query = (BulkChangeQuery.create().issues(issueKeys).actions("set_severity").actionParameter("set_severity", "severity", newSeverity));
    BulkChange bulkChange = adminIssueClient().bulkChange(query);
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(nbIssues);

    // Re apply the same bulk change ->  no issue should be changed
    bulkChange = adminIssueClient().bulkChange(query);
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(0);
    assertThat(bulkChange.totalIssuesNotChanged()).isEqualTo(nbIssues);
  }

  @Test
  public void should_not_apply_bulk_change_if_no_issue_selected() {
    BulkChangeQuery query = (BulkChangeQuery.create().actions("set_severity").actionParameter("set_severity", "severity", "BLOCKER"));
    try {
      adminIssueClient().bulkChange(query);
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void should_not_apply_bulk_change_if_action_is_invalid() {
    analyzeSampleProjectWillSmallIssues();
    int nbIssues = 3;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);

    BulkChangeQuery query = (BulkChangeQuery.create().issues(issueKeys).actions("invalid"));
    try {
      adminIssueClient().bulkChange(query);
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void should_apply_bulk_change_be_paginated() {
    analyzeProjectWithALotOfIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 120;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create()).list(), nbIssues);

    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("set_severity")
        .actionParameter("set_severity", "severity", newSeverity)
    );
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(100);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.severity()).isEqualTo(newSeverity);
    }
  }

  @Test
  @Ignore("Number of issues sent is too big (it fails from 159 issues), the bulk change execution returns a 413 HTTP code. Should find a way to not sent params in url but in the body")
  public void should_apply_bulk_change_with_maximum_number_of_issues() {
    analyzeProjectWithALotOfIssues();
    String newSeverity = "BLOCKER";
    int nbIssues = 510;
    String[] issueKeys = getIssueKeys(search(IssueQuery.create().pageSize(-1)).list(), nbIssues);

    BulkChange bulkChange = adminIssueClient().bulkChange(
      BulkChangeQuery.create()
        .issues(issueKeys)
        .actions("set_severity")
        .actionParameter("set_severity", "severity", newSeverity)
    );
    assertThat(bulkChange.totalIssuesChanged()).isEqualTo(500);
    for (Issue issue : search(IssueQuery.create().issues(issueKeys)).list()) {
      assertThat(issue.severity()).isEqualTo(newSeverity);
    }
  }

  private void analyzeSampleProjectWillSmallIssues() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));
  }

  private void analyzeProjectWithALotOfIssues() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("issue/file-with-thousands-issues"))
      .setProfile("one-issue-per-line"));
  }

  private String[] getIssueKeys(List<Issue> issues, int nbIssues) {
    Iterable<Issue> subIssues = Iterables.limit(issues, nbIssues);
    return (newArrayList(Iterables.transform(subIssues, new Function<Issue, String>() {
      public String apply(Issue issue) {
        return issue.key();
      }
    }))).toArray(new String[]{});
  }
}
