/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.permission.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

public class GlobalPermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;

  /**
   * SONAR-4412
   */
  @Test
  public void manage_user_permissions_using_ws() throws Exception {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    client.userClient().create(
      UserParameters.create().login("add_permission_user").name("add_permission_user").password("password").passwordConfirmation("password"));
    client.permissionClient().addPermission(PermissionParameters.create().user("add_permission_user").permission("profileadmin"));

    client.userClient().create(
      UserParameters.create().login("add_remove_permission_user").name("add_remove_permission_user").password("password").passwordConfirmation("password"));

    try {
      PermissionParameters secondUserProfileAdminParams = PermissionParameters.create().user("add_remove_permission_user").permission("profileadmin");
      client.permissionClient().addPermission(secondUserProfileAdminParams);
      client.permissionClient().addPermission(PermissionParameters.create().user("add_remove_permission_user").permission("shareDashboard"));
      client.permissionClient().removePermission(secondUserProfileAdminParams);

      // As there's no WS to read permissions, check is done by Selenium

      // We check that add_permission_user has the profileadmin permission, and that add_remove_permission_user hasn't the shareDashboard permission
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-users-permissions-using-ws",
        "/selenium/permission/permission-administration/manage-users-permission-with-ws.html")
        .build();
      orchestrator.executeSelenese(selenese);
    } finally {
      // Deactivate users
      client.userClient().deactivate("add_permission_user");
      client.userClient().deactivate("add_remove_permission_user");
    }
  }

  /**
   * SONAR-4412
   */
  @Test
  public void manage_groups_permissions_using_ws() throws Exception {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    PermissionParameters usersGroupProfileAdminParams = PermissionParameters.create().group("sonar-users").permission("shareDashboard");
    client.permissionClient().addPermission(usersGroupProfileAdminParams);
    client.permissionClient().removePermission(usersGroupProfileAdminParams);

    PermissionParameters usersGroupShareDashboardParams = PermissionParameters.create().group("sonar-users").permission("profileadmin");
    client.permissionClient().addPermission(usersGroupShareDashboardParams);

    try {
      // As there's no WS to read permissions, check is done by Selenium

      // We check that sonar-users has the profileadmin permission but hasn't the shareDashboard permission
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-groups-permissions-using-ws",
        "/selenium/permission/permission-administration/manage-groups-permission-with-ws.html")
        .build();
      orchestrator.executeSelenese(selenese);
    } finally {
      // Restore global permissions state
      client.permissionClient().removePermission(usersGroupProfileAdminParams);
      client.permissionClient().removePermission(usersGroupShareDashboardParams);
    }
  }

  @Test
  public void sort_by_name() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should_sort_by_name",
      "/selenium/permission/permission-administration/sort-global-permissions-by-name.html")
      .build();
    orchestrator.executeSelenese(selenese);

  }
}
