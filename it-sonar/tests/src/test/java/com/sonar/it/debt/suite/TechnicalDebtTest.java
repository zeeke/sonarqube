/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TechnicalDebtTest {

  @ClassRule
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  @Before
  public void deleteAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();

    // Set hours in day property to 8
    orchestrator.getServer().getAdminWsClient().update(
      new PropertyUpdateQuery("sonar.technicalDebt.hoursInDay", "8"));
  }

  /**
   * SONAR-4716
   */
  @Test
  public void technical_debt_on_issue() throws Exception {
    // Generate some issues
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis()
        .setProfile("one-issue-per-line"));

    // All the issues should have a technical debt
    List<Issue> issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).list();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
//      assertThat(issue.technicalDebt()).isNotNull();
//      assertThat(issue.technicalDebt().days()).isEqualTo(0);
//      assertThat(issue.technicalDebt().hours()).isEqualTo(0);
//      assertThat(issue.technicalDebt().minutes()).isEqualTo(1);
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

  @Test
  public void use_hours_in_day_property_to_display_debt() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.getServer().getAdminWsClient().update(
      // One day -> 10 hours
      new PropertyUpdateQuery("sonar.technicalDebt.hoursInDay", "10"));

    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
        // As OneIssuePerFile has a debt of 10 minutes, we multiply it by 72 to have 1 day and 2 hours of technical debt
      .setProperties("sonar.oneIssuePerFile.effortToFix", "72")
    );

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);

//    WorkDayDuration technicalDebt = issue.technicalDebt();
//    assertThat(technicalDebt.minutes()).isEqualTo(0);
//    assertThat(technicalDebt.hours()).isEqualTo(2);
//    assertThat(technicalDebt.days()).isEqualTo(1);
  }

  @Test
  public void use_hours_in_day_property_during_analysis_to_convert_debt() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-day-debt-per-file.xml"));

    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-day-debt-per-file")
        // One day -> 10 hours : debt will be stored as 360.000 seconds (1 day * 10 hours per day * 60 * 60)
      .setProperties("sonar.technicalDebt.hoursInDay", "10")
    );

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);

    // Issue debt was 1 day during analysis but will be displayed as 1 day and 2 hours (hours in day property was set to 10 during analysis but is 8 in the ui (default value))
//    WorkDayDuration technicalDebt = issue.technicalDebt();
//    assertThat(technicalDebt.days()).isEqualTo(1);
//    assertThat(technicalDebt.hours()).isEqualTo(2);
//    assertThat(technicalDebt.minutes()).isEqualTo(0);
  }

  @Test
  public void fail_when_set_effort_to_fix_on_constant_issue_requirement() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/has-hello-tag.xml"));
    BuildResult result = orchestrator.executeBuildQuietly(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("has-tag")
      .setProperties("sonar.hasTag.effortToFix", "720")
    );
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs())
      .contains("Requirement for 'xoo:HasTag' can not use 'Constant/issue' remediation function because this rule does not have a fixed remediation cost.");
  }

}
