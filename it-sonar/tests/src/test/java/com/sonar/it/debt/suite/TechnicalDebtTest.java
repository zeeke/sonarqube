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
import org.sonar.wsclient.issue.*;

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
  public void technical_debt_on_issue() throws Exception {
    // Generate some issues
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis()
        .setProfile("one-issue-per-line"));

    // All the issues should have a technical debt
    List<Issue> issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.technicalDebt()).isNotNull();
      assertThat(issue.technicalDebt().days()).isEqualTo(0);
      assertThat(issue.technicalDebt().hours()).isEqualTo(0);
      assertThat(issue.technicalDebt().minutes()).isEqualTo(1);
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

  /**
   * SONAR-4834
   */
  @Test
  public void add_technical_debt_in_issue_changelog() throws Exception {
    // Execute an analysis in the past to have a past snapshot
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file"));

    // Second analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);
    List<IssueChange> changes = issueClient.changes(issue.key());

    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);

    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("technicalDebt");

    WorkDayDuration oldValue = (WorkDayDuration) changeDiff.oldValue();
    assertThat(oldValue.minutes()).isEqualTo(10);
    assertThat(oldValue.hours()).isEqualTo(0);
    assertThat(oldValue.days()).isEqualTo(0);

    WorkDayDuration newValue = (WorkDayDuration) changeDiff.newValue();
    assertThat(newValue.minutes()).isEqualTo(40);
    assertThat(newValue.hours()).isEqualTo(1);
    assertThat(newValue.days()).isEqualTo(0);
  }

  @Test
  public void technical_debt_should_use_hours_in_day_to_convert_days() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
        // As OneIssuePerFile has a debt of 10 minutes, we multiply it by 6 * 10 (1 day) + 60 * 2 (2 hours) to have 1 day and 2 hours of technical debt
      .setProperties("sonar.oneIssuePerFile.effortToFix", "72")
        // One day -> 10 hours
      .setProperties("sonar.technicalDebt.hoursInDay", "10"));

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);

    WorkDayDuration technicalDebt = issue.technicalDebt();
    assertThat(technicalDebt.minutes()).isEqualTo(0);
    assertThat(technicalDebt.hours()).isEqualTo(2);
    assertThat(technicalDebt.days()).isEqualTo(1);
  }

  @Test
  public void fail_when_set_effort_to_fix_on_constant_issue_requirement() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    BuildResult result = orchestrator.executeBuildQuietly(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line")
      .setProperties("sonar.oneIssuePerLine.effortToFix", "720")
    );
    assertThat(result.getStatus()).isNotEqualTo(0);
    // with the following message
    assertThat(result.getLogs())
      .contains("Requirement for 'xoo:OneIssuePerLine' can not use 'Constant/issue' remediation function because this rule does not have a fixed remediation cost.");
  }

}
