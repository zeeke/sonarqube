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
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TechnicalDebtTest {

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
  public void set_technical_debt_on_issue() throws Exception {
    // Generate some issues
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis()
        .setProfile("one-issue-per-line"));

    // All the issues should have a technical debt
    List<Issue> issues = ItUtils.newWsClientForAnonymous(orchestrator).issueClient().find(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.technicalDebt()).isNotNull();
      assertThat(issue.technicalDebt().days()).isEqualTo(0);
      assertThat(issue.technicalDebt().hours()).isEqualTo(1);
      assertThat(issue.technicalDebt().minutes()).isEqualTo(0);
    }
  }

  /**
   * SONAR-4716
   */
  @Test
  public void display_requirement_details_on_issue() throws Exception {
    // Generate some issues
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/with-many-rules.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .withoutDynamicAnalysis()
        .setProfile("with-many-rules"));

    orchestrator.executeSelenese(Selenese.builder()
      .setHtmlTestsInClasspath("display-requirement-debt-details-on-issue",
        "/selenium/debt/requirement-details/display-requirement-detail.html"
      ).build());
  }

}
