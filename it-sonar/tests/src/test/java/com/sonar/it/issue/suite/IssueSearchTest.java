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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.base.Paging;
import org.sonar.wsclient.issue.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class IssueSearchTest extends AbstractIssueTestCase {

  private static int DEFAULT_PAGINATED_RESULTS = 100;

  @BeforeClass
  public static void scanStruts() {
    orchestrator.getDatabase().truncateInspectionTables();
    deleteManualRules();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "true")
      .setProfile("issues"));

    // Assign a issue to test search by assignee
    adminIssueClient().assign(searchRandomIssue().key(), "admin");

    // Resolve a issue to test search by status and by resolution
    adminIssueClient().doTransition(searchRandomIssue().key(), "resolve");

    // Create a manual issue to test search by reporter
    createManualRule();
    adminIssueClient().create(NewIssue.create().component("org.apache.struts:struts-tiles:org.apache.struts.tiles.xmlDefinition.XmlParser")
      .rule("manual:invalidclassname")
      .line(3)
      .severity("CRITICAL")
      .message("The name 'Sample' is too generic"));
  }

  @AfterClass
  public static void purgeManualRules(){
    try {
      deleteManualRules();
    } catch (Exception e) {
      // do not fail in test finalizers
      e.printStackTrace();
    }
  }

  @Test
  public void search_issues_by_component_roots() {
    Issues issues = search(IssueQuery.create().componentRoots("org.apache.struts:struts-parent"));
    assertThat(issues.list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().componentRoots("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_components() {
    assertThat(search(IssueQuery.create().components("org.apache.struts:struts-tiles:org.apache.struts.tiles.xmlDefinition.XmlDefinitionsSet")).list()).hasSize(10);
    assertThat(search(IssueQuery.create().components("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_severities() {
    assertThat(search(IssueQuery.create().severities("MAJOR")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().severities("CRITICAL")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().severities("BLOCKER")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_statuses() {
    assertThat(search(IssueQuery.create().statuses("OPEN")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().statuses("RESOLVED")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().statuses("CLOSED")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_resolutions() {
    assertThat(search(IssueQuery.create().resolutions("FIXED")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().resolutions("FALSE-POSITIVE")).list()).isEmpty();
    assertThat(search(IssueQuery.create().resolved(true)).list()).hasSize(1);
    assertThat(search(IssueQuery.create().resolved(false)).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
  }

  @Test
  public void search_issues_by_assignees() {
    assertThat(search(IssueQuery.create().assignees("admin")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assignees("unknown")).list()).isEmpty();
    assertThat(search(IssueQuery.create().assigned(true)).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assigned(false)).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
  }

  @Test
  public void search_issues_by_reporters() {
    assertThat(search(IssueQuery.create().reporters("admin")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().reporters("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_rules() {
    assertThat(search(IssueQuery.create().rules("checkstyle:com.puppycrawl.tools.checkstyle.checks.design.VisibilityModifierCheck")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().rules("manual:invalidclassname")).list()).hasSize(1);

    try {
      assertThat(search(IssueQuery.create().rules("unknown")).list()).isEmpty();
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 500);
    }
  }

  /**
   * SONAR-2981
   */
  @Test
  public void search_issues_by_dates() {
    // issues have been created today
    Date today = toDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    Date past = toDate("2013-01-01");
    Date future = toDate("2020-12-31");

    // after date
    assertThat(search(IssueQuery.create().createdAfter(future)).list()).isEmpty();
    assertThat(search(IssueQuery.create().createdAfter(today)).list().size()).isGreaterThan(0);
    assertThat(search(IssueQuery.create().createdAfter(past)).list().size()).isGreaterThan(0);

    // before
    assertThat(search(IssueQuery.create().createdBefore(future)).list().size()).isGreaterThan(0);
    assertThat(search(IssueQuery.create().createdBefore(past)).list()).isEmpty();

    // before and after
    assertThat(search(IssueQuery.create().createdBefore(future).createdAfter(past)).list().size()).isGreaterThan(0);
    assertThat(search(IssueQuery.create().createdBefore(future).createdAfter(future)).list()).isEmpty();
  }

  @Test
  public void search_issues_by_action_plans() {
    // Create an action plan
    ActionPlan actionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project("org.apache.struts:struts-parent").description("Short term issues")
      .deadLine(toDate("2113-01-31")));

    // Associate this action plan to an issue
    adminIssueClient().plan(searchRandomIssue().key(), actionPlan.key());

    assertThat(search(IssueQuery.create().actionPlans(actionPlan.key())).list()).hasSize(1);
    assertThat(search(IssueQuery.create().actionPlans("unknown")).list()).isEmpty();
  }

  @Test
  public void should_paginate_results() {
    Issues issues = search(IssueQuery.create().pageSize(20).pageIndex(2));

    assertThat(issues.list()).hasSize(20);
    Paging paging = issues.paging();
    assertThat(paging.pageIndex()).isEqualTo(2);
    assertThat(paging.pageSize()).isEqualTo(20);
    assertThat(paging.pages()).isEqualTo(206);
    assertThat(paging.total()).isEqualTo(4117);
    assertThat(issues.maxResultsReached()).isFalse();

    // SONAR-3257
    // return max page size results when using negative page size value
    assertThat(search(IssueQuery.create().pageSize(0)).list()).hasSize(500);
    assertThat(search(IssueQuery.create().pageSize(-1)).list()).hasSize(500);
  }

  @Test
  public void should_sort_results() {
    // Only 1 issue in CRITICAL (the manual one), following ones are in MAJOR
    List<Issue> issues = search(IssueQuery.create().sort("SEVERITY").asc(false)).list();
    assertThat(issues.get(0).severity()).isEqualTo("CRITICAL");
    issues.remove(0);
    for (Issue issue : issues) {
      assertThat(issue.severity()).isEqualTo("MAJOR");
    }
  }

  /**
   * SONAR-4563
   */
  @Test
  public void should_search_by_exact_creation_date() {
    final Issue issue = search(IssueQuery.create()).list().get(0);
    assertThat(issue.creationDate()).isNotNull();

    // search the issue key with the same date
    List<Issue> issues = search(IssueQuery.create().issues().issues(issue.key()).createdAt(issue.creationDate())).list();
    assertThat(issues).hasSize(1);

    // search with future and past dates that do not match any issues
    assertThat(search(IssueQuery.create().createdAt(toDate("2020-01-01"))).size()).isEqualTo(0);
    assertThat(search(IssueQuery.create().createdAt(toDate("2010-01-01"))).size()).isEqualTo(0);
  }

  /**
   * SONAR-4301
   */
  @Test
  public void search_issues_from_ui() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("execute-issue-filters",
      "/selenium/issue/issues-search/link-from-main-header.html",
      "/selenium/issue/issues-search/initial-search-form.html",
      "/selenium/issue/issues-search/search-by-project.html",
      "/selenium/issue/issues-search/search-by-severities.html",
      "/selenium/issue/issues-search/search-by-statuses.html",
      "/selenium/issue/issues-search/search-by-resolutions.html",
      "/selenium/issue/issues-search/search-by-assignee.html",
      "/selenium/issue/issues-search/search-by-reporter.html",
      "/selenium/issue/issues-search/search-by-creation-date.html",
      "/selenium/issue/issues-search/should-description-link-on-issue-detail.html",
      "/selenium/issue/issues-search/result-should-be-paginated.html",
      "/selenium/issue/issues-search/should-sort-by-severity.html",
      "/selenium/issue/issues-search/should-sort-by-status.html",
      "/selenium/issue/issues-search/should-sort-by-assignee.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

}
