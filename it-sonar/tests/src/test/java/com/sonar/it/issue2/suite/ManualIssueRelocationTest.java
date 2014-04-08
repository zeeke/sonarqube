/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;

import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ManualIssueRelocationTest extends AbstractIssueTestCase2 {

  private final static String COMPONENT_KEY = "sample:src/main/xoo/sample/Sample.xoo";
  private static final String OLD_DATE = "2010-01-01";
  private static final String NEW_DATE = "2013-05-18";

  private Date issueCreationDate;

  @BeforeClass
  public static void initManualRule() {
    createManualRule();
  }

  @AfterClass
  public static void purgeManualRules() {
    deleteManualRules();
  }

  @Before
  public void before() {
    orchestrator.getDatabase().truncateInspectionTables();
    analyzeInitialProject();
    createManualIssue();
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_move_manual_issue_if_same_line_found() {
    analyzeModifiedProject("issue/xoo-sample-v2");
    checkManualIssueOpenAt(6);
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_not_touch_issue_if_same_line_not_found() {
    analyzeModifiedProject("issue/xoo-sample-v3");
    checkManualIssueOpenAt(3);
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_not_touch_issue_if_same_line_found_multiple_times() {
    analyzeModifiedProject("issue/xoo-sample-v4");
    checkManualIssueOpenAt(3);
  }

  /**
   * SONAR-3387
   */
  @Test
  public void should_close_issue_if_same_line_not_found_and_old_line_out_of_new_source() {
    analyzeModifiedProject("issue/xoo-sample-v5");
    checkManualIssueClosed();
  }

  private void checkManualIssueOpenAt(int line) {
    checkManualIssueStatus(line, "OPEN", null);
  }

  private void checkManualIssueClosed() {
    checkManualIssueStatus(null, "CLOSED", "REMOVED");
  }

  private void checkManualIssueStatus(Integer line, String status, String resolution) {
    List<Issue> issues = searchIssuesByComponent(COMPONENT_KEY);
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.ruleKey()).isEqualTo("manual:invalidclassname");
    if (line == null) {
      assertThat(issue.line()).isNull();
      ;
    } else {
      assertThat(issue.line()).isEqualTo(line);
    }
    assertThat(issue.severity()).isEqualTo(("MAJOR"));
    assertThat(issue.message()).isEqualTo(("The name 'Sample' is too generic"));
    assertThat(issue.status()).isEqualTo(status);
    if (resolution == null) {
      assertThat(issue.resolution()).isNull();
    } else {
      assertThat(issue.resolution()).isEqualTo(resolution);
    }
    assertThat(issue.creationDate()).isEqualTo(issueCreationDate);
    assertThat(issue.updateDate()).isNotNull();
    assertThat(issue.reporter()).isEqualTo("admin");
  }

  private void analyzeInitialProject() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    // no active rules
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true",
        "sonar.projectDate", OLD_DATE)
      .setProfile("empty");
    orchestrator.executeBuild(runner);
  }

  private void analyzeModifiedProject(String project) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(project))
      .setProperties(
        "sonar.cpd.skip", "true",
        "sonar.projectDate", NEW_DATE)
      .setProfile("empty");
    orchestrator.executeBuild(runner);
  }

  private void createManualIssue() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("manual-issues-on-line",
      "/selenium/issue/manual-issue/create-manual-issue-on-line.html"
      ).build());
    this.issueCreationDate = searchIssuesByComponent(COMPONENT_KEY).get(0).creationDate();
  }
}
