/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.ActionPlanClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public abstract class AbstractIssueTestCase {

  @ClassRule
  public static Orchestrator orchestrator = IssueTestSuite.ORCHESTRATOR;

  protected Issue searchIssueByKey(String issueKey) {
    List<Issue> issues = search(IssueQuery.create().issues(issueKey)).list();
    assertThat(issues).hasSize(1);
    return issues.get(0);
  }

  protected List<Issue> searchIssuesBySeverities(String componentKey, String... severities) {
    return search(IssueQuery.create().components(componentKey).severities(severities)).list();
  }

  protected static Issue searchRandomIssue() {
    List<Issue> issues = search(IssueQuery.create()).list();
    assertThat(issues).isNotEmpty();
    return issues.get(0);
  }

  protected static Issues search(IssueQuery issueQuery) {
    return issueClient().find(issueQuery);
  }

  protected static IssueClient issueClient() {
    return orchestrator.getServer().wsClient().issueClient();
  }

  protected static IssueClient adminIssueClient() {
    return orchestrator.getServer().adminWsClient().issueClient();
  }

  protected static ActionPlanClient actionPlanClient() {
    return orchestrator.getServer().wsClient().actionPlanClient();
  }

  protected static ActionPlanClient adminActionPlanClient() {
    return orchestrator.getServer().adminWsClient().actionPlanClient();
  }

  protected static ActionPlan firstActionPlan(String projectKey) {
    List<ActionPlan> actionPlans = actionPlanClient().find(projectKey);
    assertThat(actionPlans).hasSize(1);
    return actionPlans.get(0);
  }

  protected void verifyHttpException(Exception e, int expectedCode) {
    assertThat(e).isInstanceOf(HttpException.class);
    HttpException exception = (HttpException) e;
    assertThat(exception.status()).isEqualTo(expectedCode);
  }

}
