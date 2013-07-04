/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ActionPlanTest.class, IssueActionTest.class, IssueFilterExtensionTest.class, IssueJsonReportTest.class,
  IssuePurgeTest.class, IssueTest.class, IssueTrackingTest.class, IssueWorkflowTest.class,
  ManualIssueTest.class, IssueWidgetsTest.class, IssueSearchTest.class, IssueFiltersTest.class, IssueBulkChangeTest.class
})
public class IssueTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.locateTestPlugin("issue-filter-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("issue-action-plugin"))
    .build();
}