/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.permission.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.*;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

public class DashboardSharingPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUpUsers() {
    orchestrator.getDatabase().truncateInspectionTables();

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    client.userClient().create(UserParameters.create().login("dashboard_user").name("dashboard_user")
      .password("password").passwordConfirmation("password"));
    client.userClient().create(UserParameters.create().login("can_share_dashboards").name("can_share_dashboards")
      .password("password").passwordConfirmation("password"));
    client.userClient().create(UserParameters.create().login("cannot_share_dashboards").name("cannot_share_dashboards")
      .password("password").passwordConfirmation("password"));

    client.permissionClient().addPermission(PermissionParameters.create().user("can_share_dashboards").permission("shareDashboard"));
  }

  @AfterClass
  public static void deactivateUsers() {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    client.userClient().deactivate("dashboard_user");
    client.userClient().deactivate("can_share_dashboards");
    client.userClient().deactivate("cannot_share_dashboards");
  }

  /**
   * SONAR-4099
   */
  @Test
  @Ignore("Too many false positives with ui")
  public void enable_dashboard_sharing() throws Exception {
    try {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("set-dashboard-sharing-permission",
        "/selenium/permission/dashboard-sharing-permission/user-dashboard-sharing-permission.html",
        "/selenium/permission/dashboard-sharing-permission/group-dashboard-sharing-permission.html")
        .build();
      orchestrator.executeSelenese(selenese);
    } finally {
      // Restore global permissions state that have been changed by selenium tests
      ItUtils.newWsClientForAdmin(orchestrator).permissionClient().removePermission(
        PermissionParameters.create().group("sonar-users").permission("shareDashboard")
      );
    }
  }

  /**
   * SONAR-4136
   */
  @Test
  public void share_global_dashboard() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("global-dashboard-sharing-permission",
      "/selenium/permission/dashboard-sharing-permission/global-dashboard-sharing-allowed.html",
      "/selenium/permission/dashboard-sharing-permission/global-dashboard-sharing-denied.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4136
   */
  @Test
  public void share_project_dashboard() throws Exception {
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-dashboard-sharing-permission",
      "/selenium/permission/dashboard-sharing-permission/project-dashboard-sharing-allowed.html",
      "/selenium/permission/dashboard-sharing-permission/project-dashboard-sharing-denied.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
