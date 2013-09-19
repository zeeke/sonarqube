/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  IssueRuleWidgetsTest.class, IssueSearchTest.class, IssueTest.class, IssueTrackingTest.class,
  IssueWidgetsTest.class, IssueWorkflowTest.class, ManualIssueTest.class, IssuePermissionTest.class
})
public class Issue2TestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();
}
