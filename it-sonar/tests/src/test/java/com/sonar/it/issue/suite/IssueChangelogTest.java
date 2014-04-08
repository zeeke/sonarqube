/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueChange;
import org.sonar.wsclient.issue.IssueChangeDiff;

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
  public void update_changelog_when_assigning_issue_by_user() throws Exception {
    assertThat(issueClient().changes(issue.key())).isEmpty();
    adminIssueClient().assign(issue.key(), "admin");

    List<IssueChange> changes = issueClient().changes(issue.key());
    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);
    assertThat(change.user()).isEqualTo("admin");
    assertThat(change.creationDate()).isNotNull();
    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("assignee");
    assertThat(changeDiff.oldValue()).isNull();
    assertThat(changeDiff.newValue()).isEqualTo("Administrator");
  }

  @Test
  public void update_changelog_when_reopening_unresolved_issue_by_scan() throws Exception {
    assertThat(issueClient().changes(issue.key())).isEmpty();

    // re analyse the project after resolving an issue in order to reopen it
    adminIssueClient().doTransition(issue.key(), "resolve");
    orchestrator.executeBuild(scan);

    List<IssueChange> changes = issueClient().changes(issue.key());
    assertThat(changes).hasSize(2);

    // Change done by the user (first change is be the oldest one)
    IssueChange change1 = changes.get(0);
    assertThat(change1.user()).isEqualTo("admin");
    assertThat(change1.creationDate()).isNotNull();
    assertThat(change1.diffs()).hasSize(2);

    IssueChangeDiff change1Diff1 = change1.diffs().get(0);
    assertThat(change1Diff1.key()).isEqualTo("resolution");
    assertThat(change1Diff1.oldValue()).isNull();
    assertThat(change1Diff1.newValue()).isEqualTo("FIXED");

    IssueChangeDiff change1Diff2 = change1.diffs().get(1);
    assertThat(change1Diff2.key()).isEqualTo("status");
    assertThat(change1Diff2.oldValue()).isEqualTo("OPEN");
    assertThat(change1Diff2.newValue()).isEqualTo("RESOLVED");

    // Change done by scan
    IssueChange change2 = changes.get(1);
    assertThat(change2.user()).isNull();
    assertThat(change2.creationDate()).isNotNull();
    assertThat(change2.diffs()).hasSize(2);

    IssueChangeDiff changeDiff1 = change2.diffs().get(0);
    assertThat(changeDiff1.key()).isEqualTo("resolution");
    assertThat(changeDiff1.oldValue()).isNull();
    assertThat(changeDiff1.newValue()).isNull();

    IssueChangeDiff changeDiff2 = change2.diffs().get(1);
    assertThat(changeDiff2.key()).isEqualTo("status");
    assertThat(changeDiff2.oldValue()).isEqualTo("RESOLVED");
    assertThat(changeDiff2.newValue()).isEqualTo("REOPENED");
  }
}
