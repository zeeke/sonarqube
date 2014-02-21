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
import org.junit.Test;
import org.sonar.wsclient.issue.*;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;


/**
 * SONAR-4834
 */
public class TechnicalDebtInIssueChangelogTest {

  @ClassRule
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  @Before
  public void deleteAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();

    // Set hours in day property to 8
    orchestrator.getServer().getAdminWsClient().update(
      new PropertyUpdateQuery("sonar.technicalDebt.hoursInDay", "8"));
  }

  @Test
  public void display_debt_in_issue_changelog() throws Exception {
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
    assertThat(oldValue.days()).isEqualTo(0);
    assertThat(oldValue.hours()).isEqualTo(0);
    assertThat(oldValue.minutes()).isEqualTo(10);

    WorkDayDuration newValue = (WorkDayDuration) changeDiff.newValue();
    assertThat(newValue.days()).isEqualTo(0);
    assertThat(newValue.hours()).isEqualTo(1);
    assertThat(newValue.minutes()).isEqualTo(40);
  }

  @Test
  public void use_hours_in_day_property_to_display_debt_in_issue_changelog() throws Exception {
    // Execute an analysis in the past to have a past snapshot
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file"));

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
    List<IssueChange> changes = issueClient.changes(issue.key());

    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);

    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("technicalDebt");

    WorkDayDuration oldValue = (WorkDayDuration) changeDiff.oldValue();
    assertThat(oldValue.days()).isEqualTo(0);
    assertThat(oldValue.hours()).isEqualTo(0);
    assertThat(oldValue.minutes()).isEqualTo(10);

    WorkDayDuration newValue = (WorkDayDuration) changeDiff.newValue();
    assertThat(newValue.days()).isEqualTo(1);
    assertThat(newValue.hours()).isEqualTo(2);
    assertThat(newValue.minutes()).isEqualTo(0);
  }

}
