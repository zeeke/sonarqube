/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

public class SystemAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUpUsers() {
    orchestrator.getDatabase().truncateInspectionTables();

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters userCreationParameters = UserParameters.create().login("not_admin_user").name("Not a system admin")
      .password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);
    UserParameters canShareDashboard = UserParameters.create().login("can_share").name("can_share")
      .password("password").passwordConfirmation("password");
    client.userClient().create(canShareDashboard);
    UserParameters cannotShareDashboard = UserParameters.create().login("cannot_share").name("cannot_share")
      .password("password").passwordConfirmation("password");
    client.userClient().create(cannotShareDashboard);
    PermissionParameters dashboardSharing = PermissionParameters.create().user("can_share").permission("shareDashboard");
    client.permissionClient().addPermission(dashboardSharing);
  }

  /**
   * SONAR-4398
   */
  @Test
  public void should_change_ownership_of_shared_measure_filter() throws Exception {
    seleniumSuite("change-measure-filter-ownership",
      "/selenium/administration/system-admin-permission/change-own-measure-filter-owner.html",
      "/selenium/administration/system-admin-permission/change-other-measure-filter-owner.html",
      "/selenium/administration/system-admin-permission/change-system-measure-filter-owner.html");
  }

  /**
   * SONAR-4399
   */
  @Test
  public void should_change_ownership_of_shared_issue_filter() throws Exception {
    seleniumSuite("change-issue-filter-ownership",
      "/selenium/administration/system-admin-permission/change-own-issue-filter-owner.html",
      "/selenium/administration/system-admin-permission/change-other-issue-filter-owner.html");
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_change_ownership_of_shared_global_dashboard() throws Exception {
    seleniumSuite("change-global-dashboard-ownership",
      "/selenium/administration/system-admin-permission/change-shared-global-dashboard-owner.html",
      "/selenium/administration/system-admin-permission/change-shared-global-dashboard-owner-failure.html");
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_change_ownership_of_shared_project_dashboard() throws Exception {
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/sample")));

    seleniumSuite("change-project-dashboard-ownership",
      "/selenium/administration/system-admin-permission/change-shared-project-dashboard-owner.html",
      "/selenium/administration/system-admin-permission/change-shared-project-dashboard-owner-failure.html");
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }
}
