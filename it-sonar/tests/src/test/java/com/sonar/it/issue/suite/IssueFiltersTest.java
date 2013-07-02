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
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;

/**
 * SONAR-4383
 */
public class IssueFiltersTest extends AbstractIssueTestCase {

  @BeforeClass
  public static void scanProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(runner);

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    UserParameters userCreationParameters = UserParameters.create().login("user-issue-filters").name("User Issue Filters").password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);
  }

  /**
   * SONAR-4391
   */
  @Test
  public void should_save_filter() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_save_filter",
      "/selenium/issue/issue-filters/should-save-issue-filters.html",
      "/selenium/issue/issue-filters/should-save-issue-filters-with-description.html",
      "/selenium/issue/issue-filters/should-not-save-filter-with-already-used-name.html"
    ).build());
  }

  /**
   * SONAR-4392
   */
  @Test
  public void should_update_filter() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_update_filter",
      "/selenium/issue/issue-filters/should-update-issue-filters.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-description-from-search.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-description-from-manage.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-title-from-search.html",
      "/selenium/issue/issue-filters/should-update-issue-filters-title-from-manage.html"
    ).build());
  }

  /**
   * SONAR-4392
   */
  @Test
  public void should_copy_filter() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_copy_filter",
      "/selenium/issue/issue-filters/should-copy-filter.html",
      "/selenium/issue/issue-filters/should-copy-filter-from-manage.html",
      "/selenium/issue/issue-filters/should-not-copy-filter-with-already-existing-name.html"
    ).build());
  }

  /**
   * SONAR-4392
   */
  @Test
  public void should_delete_filter() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_delete_filter",
      "/selenium/issue/issue-filters/should-delete-filter.html"
    ).build());
  }

  /**
   * SONAR-4393
   */
  @Test
  public void should_flag_as_favorite() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_flag_as_favorite",
      "/selenium/issue/issue-filters/should-flag-as-favorite-when-saving-filter.html",
      "/selenium/issue/issue-filters/should-unflag-as-favorite.html"
    ).build());
  }

  /**
   * SONAR-4394
   */
  @Test
  public void should_share_filter() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_share_filter",
      "/selenium/issue/issue-filters/should-share-filter.html",
      "/selenium/issue/issue-filters/should-share-filter-from-manage.html",
      "/selenium/issue/issue-filters/should-open-filter-shared-by-another-user.html",
      "/selenium/issue/issue-filters/should-save-none-shared-filter-with-name-already-used-by-shared-filter.html",
      "/selenium/issue/issue-filters/should-not-save-shared-filter-with-name-already-used-by-shared-filter.html",
      "/selenium/issue/issue-filters/should-flag-as-favorite-filter-shared-by-another-user.html",
      "/selenium/issue/issue-filters/should-copy-filter-shared-by-another-user.html",
      "/selenium/issue/issue-filters/admin-should-edit-filter-shared-by-others.html",
      // SONAR-4469
      "/selenium/issue/issue-filters/should-unshare-filter-remove-other-filters-favourite.html"
    ).build());
  }
}
