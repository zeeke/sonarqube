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

public class DashboardSharingPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUpUsers() {
    orchestrator.getDatabase().truncateInspectionTables();

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    UserParameters newUser = UserParameters.create().login("dashboard_user").name("dashboard_user")
      .password("password").passwordConfirmation("password");
    client.userClient().create(newUser);
    UserParameters canShareDashboard = UserParameters.create().login("can_share_dashboards").name("can_share_dashboards")
      .password("password").passwordConfirmation("password");
    client.userClient().create(canShareDashboard);
    UserParameters cannotShareDashboard = UserParameters.create().login("cannot_share_dashboards").name("cannot_share_dashboards")
      .password("password").passwordConfirmation("password");
    client.userClient().create(cannotShareDashboard);
    PermissionParameters dashboardSharing = PermissionParameters.create().user("can_share_dashboards").permission("shareDashboard");
    client.permissionClient().addPermission(dashboardSharing);
  }

  /**
   * SONAR-4099
   */
  @Test
  public void should_enable_dashboard_sharing() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("set-dashboard-sharing-permission",
      "/selenium/administration/dashboard-sharing-permission/user-dashboard-sharing-permission.html",
      "/selenium/administration/dashboard-sharing-permission/group-dashboard-sharing-permission.html")
    .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_share_global_dashboard() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("global-dashboard-sharing-permission",
      "/selenium/administration/dashboard-sharing-permission/global-dashboard-sharing-allowed.html",
      "/selenium/administration/dashboard-sharing-permission/global-dashboard-sharing-denied.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4136
   */
  @Test
  public void should_share_project_dashboard() throws Exception {
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/sample")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-dashboard-sharing-permission",
      "/selenium/administration/dashboard-sharing-permission/project-dashboard-sharing-allowed.html",
      "/selenium/administration/dashboard-sharing-permission/project-dashboard-sharing-denied.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
