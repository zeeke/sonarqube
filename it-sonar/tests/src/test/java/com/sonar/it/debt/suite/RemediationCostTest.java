/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RemediationCostTest {

  @ClassRule
  public static Orchestrator orchestrator = DebtTestSuite.ORCHESTRATOR;

  @Before
  public void deleteAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4716
   */
  @Test
  @Ignore("Currently fqiling becquse remediation cost seems to not be set on issues")
  public void set_remediation_cost_on_issue() throws Exception {
    // Generate some issues
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis()
        .setProfile("one-issue-per-line"));

    // All the issues have a remediation cost
    List<Issue> issues = ItUtils.newWsClientForAnonymous(orchestrator).issueClient().find(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.remediationCost()).isNotNull();
    }
  }


}
