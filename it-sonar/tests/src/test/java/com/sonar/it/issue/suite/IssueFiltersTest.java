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
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class IssueFiltersTest extends AbstractIssueTestCase {

  @BeforeClass
  public static void scanProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(runner);
  }

  @Test
  public void should_save_issue_filters() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_save_issue_filters",
      "/selenium/issue/issue-filters/should-save-issue-filters.html",
      "/selenium/issue/issue-filters/should-save-issue-filters-with-description.html"
    ).build());
  }

  @Test
  public void should_update_issue_filters() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_update_issue_filters",
      "/selenium/issue/issue-filters/should-update-issue-filters.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-description-from-search.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-description-from-manage.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-title-from-search.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-title-from-manage.html"
    ).build());
  }

  @Test
  @Ignore("TODO")
  public void should_copy_issue_filters() {
  }

  @Test
  @Ignore("TODO")
  public void should_delete_issue_filters() {
  }

  @Test
  @Ignore("TODO")
  public void should_share_issue_filters() {
  }

}
