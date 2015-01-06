/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.lang.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.base.Paging;
import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.issue.NewActionPlan;
import org.sonar.wsclient.issue.NewIssue;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class IssueSearchTest extends AbstractIssueTestCase2 {

  private static int DEFAULT_PAGINATED_RESULTS = 100;
  private static int TOTAL_NB_ISSUES = 125;

  @BeforeClass
  public static void prepareData() {
    orchestrator.resetData();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-many-rules.xml"));
    // Launch 2 analysis to have more than 100 issues in total
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProfile("with-many-rules"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.branch", "multi-modules-sample2")
      .setProfile("with-many-rules"));

    // Assign a issue to test search by assignee
    adminIssueClient().assign(searchRandomIssue().key(), "admin");

    // Resolve a issue to test search by status and by resolution
    adminIssueClient().doTransition(searchRandomIssue().key(), "resolve");

    // Create a manual issue to test search by reporter
    createManualRule();
    adminIssueClient().create(
      NewIssue.create().component("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo")
        .rule("manual:invalidclassname")
        .line(3)
        .severity("CRITICAL")
        .message("The name of the class is invalid"));
  }

  @Test
  public void search_all_issues() {
    assertThat(search(IssueQuery.create()).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
  }

  @Test
  public void search_issues_by_component_roots() {
    assertThat(search(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample")).list()).hasSize(63);
    assertThat(search(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample:module_a")).list()).hasSize(35);
    assertThat(search(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1")).list()).hasSize(20);

    assertThat(search(IssueQuery.create().components("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_components() {
    assertThat(
      search(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo")).list())
      .hasSize(19);
    assertThat(search(IssueQuery.create().components("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_severities() {
    assertThat(search(IssueQuery.create().severities("BLOCKER")).list()).isEmpty();
    assertThat(search(IssueQuery.create().severities("CRITICAL")).list()).hasSize(9);
    assertThat(search(IssueQuery.create().severities("MAJOR")).list()).hasSize(8);
    assertThat(search(IssueQuery.create().severities("MINOR")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().severities("INFO")).list()).hasSize(4);
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
    assertThat(search(IssueQuery.create().resolved(false)).paging().total()).isEqualTo(TOTAL_NB_ISSUES - 1);
  }

  @Test
  public void search_issues_by_assignees() {
    assertThat(search(IssueQuery.create().assignees("admin")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assignees("unknown")).list()).isEmpty();
    assertThat(search(IssueQuery.create().assigned(true)).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assigned(false)).paging().total()).isEqualTo(TOTAL_NB_ISSUES - 1);
  }

  @Test
  public void search_issues_by_reporters() {
    assertThat(search(IssueQuery.create().reporters("admin")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().reporters("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_rules() {
    assertThat(search(IssueQuery.create().rules("xoo:OneIssuePerLine")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().rules("xoo:OneIssuePerFile")).list()).hasSize(8);
    assertThat(search(IssueQuery.create().rules("manual:invalidclassname")).list()).hasSize(1);

    try {
      assertThat(search(IssueQuery.create().rules("unknown")).list()).isEmpty();
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  @Test
  public void not_return_rules_when_using_hide_rules_parameter() {
    assertThat(search(IssueQuery.create().hideRules(false)).rules()).isNotEmpty();
    assertThat(search(IssueQuery.create().hideRules(true)).rules()).isEmpty();
  }

  /**
   * SONAR-2981
   */
  @Test
  public void search_issues_by_dates() {
    // issues have been created today
    Date today = ItUtils.toDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    Date past = ItUtils.toDate("2013-01-01");
    Date future = ItUtils.toDate("2020-12-31");

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
    ActionPlan actionPlan = adminActionPlanClient().create(
      NewActionPlan.create().name("Short term").project("com.sonarsource.it.samples:multi-modules-sample").description("Short term issues")
        .deadLine(ItUtils.toDate("2113-01-31")));

    // Associate this action plan to an issue
    adminIssueClient().plan(searchRandomIssue().key(), actionPlan.key());

    assertThat(search(IssueQuery.create().actionPlans(actionPlan.key())).list()).hasSize(1);
    assertThat(search(IssueQuery.create().actionPlans("unknown")).list()).isEmpty();
  }

  /**
   * SONAR-5132
   */
  @Test
  public void search_issues_by_languages() {
    assertThat(search(IssueQuery.create().languages("xoo")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().languages("foo")).list()).isEmpty();
  }

  @Test
  public void paginate_results() {
    Issues issues = search(IssueQuery.create().pageSize(20).pageIndex(2));

    assertThat(issues.list()).hasSize(20);
    Paging paging = issues.paging();
    assertThat(paging.pageIndex()).isEqualTo(2);
    assertThat(paging.pageSize()).isEqualTo(20);
    assertThat(paging.pages()).isEqualTo(7);
    assertThat(paging.total()).isEqualTo(125);
    assertThat(issues.maxResultsReached()).isFalse();

    // SONAR-3257
    // return max page size results when using negative page size value
    assertThat(search(IssueQuery.create().pageSize(0)).list()).hasSize(125);
    assertThat(search(IssueQuery.create().pageSize(-1)).list()).hasSize(125);
  }

  @Test
  public void sort_results() {
    // 9 issue in CRITICAL (including the manual one), following ones are in MAJOR
    List<Issue> issues = search(IssueQuery.create().sort("SEVERITY").asc(false)).list();
    assertThat(issues.get(0).severity()).isEqualTo("CRITICAL");
    assertThat(issues.get(8).severity()).isEqualTo("CRITICAL");
    assertThat(issues.get(9).severity()).isEqualTo("MAJOR");
  }

  /**
   * SONAR-4563
   */
  @Test
  public void search_by_exact_creation_date() {
    final Issue issue = search(IssueQuery.create()).list().get(0);
    assertThat(issue.creationDate()).isNotNull();

    // search the issue key with the same date
    assertThat(search(IssueQuery.create().issues().issues(issue.key()).createdAt(issue.creationDate())).list()).hasSize(1);

    // search issue key with 1 second more and less should return nothing
    assertThat(search(IssueQuery.create().issues().issues(issue.key()).createdAt(DateUtils.addSeconds(issue.creationDate(), 1))).size()).isEqualTo(0);
    assertThat(search(IssueQuery.create().issues().issues(issue.key()).createdAt(DateUtils.addSeconds(issue.creationDate(), -1))).size()).isEqualTo(0);

    // search with future and past dates that do not match any issues
    assertThat(search(IssueQuery.create().createdAt(ItUtils.toDate("2020-01-01"))).size()).isEqualTo(0);
    assertThat(search(IssueQuery.create().createdAt(ItUtils.toDate("2010-01-01"))).size()).isEqualTo(0);
  }

  @Test
  public void components_contain_sub_project_id_and_project_id_informations() {
    String fileKey = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

    Issues issues = issueClient().find(IssueQuery.create().components(fileKey));
    assertThat(issues.list()).isNotEmpty();

    Collection<Component> components = issues.components();

    Component project = findComponent(components, "com.sonarsource.it.samples:multi-modules-sample");
    assertThat(project.subProjectId()).isNull();
    assertThat(project.projectId()).isNull();

    Component subModuleA1 = findComponent(components, "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1");
    assertThat(subModuleA1.subProjectId()).isEqualTo(project.id());
    assertThat(subModuleA1.projectId()).isEqualTo(project.id());

    Component file = findComponent(components, fileKey);
    assertThat(file.subProjectId()).isNotNull();
    assertThat(file.projectId()).isNotNull();

    Issue issue = issues.list().get(0);
    assertThat(issues.component(issue)).isNotNull();
    assertThat(issues.component(issue).subProjectId()).isEqualTo(subModuleA1.id());
    assertThat(issues.component(issue).projectId()).isEqualTo(project.id());
  }

  private static Component findComponent(Collection<Component> components, final String key){
    return Iterables.find(components, new Predicate<Component>() {
      @Override
      public boolean apply(Component input) {
        return key.equals(input.key());
      }
    });
  }

}
