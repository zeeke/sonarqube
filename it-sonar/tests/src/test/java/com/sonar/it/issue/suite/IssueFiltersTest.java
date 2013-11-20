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
import org.sonar.wsclient.permissions.PermissionParameters;
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
    SonarRunner twoLettersLongProjectScan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-two-letters-named"))
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(twoLettersLongProjectScan);

    createUser("user-issue-filters", "User Issue Filters");
  }

  /**
   * SONAR-4391
   */
  @Test
  public void should_save_filter() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_save_filter",
      "/selenium/issue/issue-filters/should-save-issue-filters.html",
      "/selenium/issue/issue-filters/should-not-save-issue-filter-if-no-name.html",
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
      "/selenium/issue/issue-filters/should-be-flagged-as-favorite-when-saving-filter.html",
      "/selenium/issue/issue-filters/should-unflag-as-favorite.html"
    ).build());
  }

  /**
   * SONAR-4394, SONAR-4099
   */
  @Test
  public void should_share_filter() {
    createUser("user-issue-filters-with-sharing-perm", "User Issue Filters with sharing permission", "shareDashboard");

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_share_filter",
      "/selenium/issue/issue-filters/should-share-filter.html",
      "/selenium/issue/issue-filters/should-share-filter-from-manage.html",
      "/selenium/issue/issue-filters/should-open-filter-shared-by-another-user.html",
      "/selenium/issue/issue-filters/should-save-none-shared-filter-with-name-already-used-by-shared-filter.html",
      "/selenium/issue/issue-filters/should-not-save-shared-filter-with-name-already-used-by-shared-filter.html",
      "/selenium/issue/issue-filters/should-flag-as-favorite-filter-shared-by-another-user.html",
      "/selenium/issue/issue-filters/should-copy-filter-shared-by-another-user.html",
      // SONAR-2474
      "/selenium/issue/issue-filters/admin-should-edit-filter-shared-by-others.html"
    ).build());

    // Test of SONAR-4099 has been dropped : remove from favourites when unsharing a filter
  }

  /**
   * SONAR-4099
   */
  @Test
  public void should_not_share_filter_when_user_have_no_sharing_permissions() {
    createUser("user-issue-filters-without-sharing-perm", "User Issue Filters without sharing permission");

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_not_share_filter_when_user_have_no_sharing_permissions",
      "/selenium/issue/issue-filters/should-not-share-filter-when-user-have-no-sharing-permissions.html"
    ).build());
  }

  /**
   * SONAR-4570
   */
  @Test
  public void should_enable_filtering_on_two_letters_long_project() throws Exception {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should_enable_filtering_on_short_project_name",
      "/selenium/issue/issue-filters/should-enable-filtering-on-short-project-name.html"
    ).build());
  }

  private static void createUser(String login, String name){
    createUser(login, name, null);
  }

  private static void createUser(String login, String name, String permission){
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    UserParameters userCreationParameters = UserParameters.create().login(login).name(name).password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);

    if (permission != null) {
      client.permissionClient().addPermission(PermissionParameters.create().user(login).permission(permission));
    }
  }
}
