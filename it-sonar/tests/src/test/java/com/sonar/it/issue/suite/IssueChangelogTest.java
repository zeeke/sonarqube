/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.*;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueChangelogTest extends AbstractIssueTestCase {

  Issue issue;
  SonarRunner scan;

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.cpd.skip", "true")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);
    issue = searchRandomIssue();
  }

  @Test
  public void update_changelog_on_assign() throws Exception {
    assertThat(issueClient().changes(issue.key())).isEmpty();
    adminIssueClient().assign(issue.key(), "admin");

    List<IssueChange> changes = issueClient().changes(issue.key());
    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);
    assertThat(change.user()).isEqualTo("admin");
    assertThat(change.createdAt()).isNotNull();
    assertThat(change.updatedAt()).isNotNull();
    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("assignee");
    assertThat(changeDiff.oldValue()).isNull();
    assertThat(changeDiff.newValue()).isEqualTo("Administrator");
  }

  /**
   * SONAR-4375
   */
  @Test
  public void display_issue_changelog_entries() throws Exception {
    ActionPlan newActionPlan = adminActionPlanClient().create(NewActionPlan.create().name("Short term").project("sample")
      .description("Short term issues").deadLine(ItUtils.toDate("2113-01-31")));
    assertThat(newActionPlan.key()).isNotNull();

    adminIssueClient().doTransition(issue.key(), "confirm");
    adminIssueClient().assign(issue.key(), "admin");
    adminIssueClient().setSeverity(issue.key(), "BLOCKER");
    adminIssueClient().plan(issue.key(), newActionPlan.key());

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("display-issue-changelog", "/selenium/issue/should-display-issue-changelog.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
