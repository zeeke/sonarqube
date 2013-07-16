/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

public class PermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @AfterClass
  public static void restoreDefaultGroupsPermissions() {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    PermissionParameters usersGroupShareDashboardParams = PermissionParameters.create().group("sonar-users").permission("shareDashboard");
    client.permissionClient().removePermission(usersGroupShareDashboardParams);
    PermissionParameters usersGroupProfileAdminParams = PermissionParameters.create().group("sonar-users").permission("profileadmin");
    client.permissionClient().removePermission(usersGroupProfileAdminParams);
  }

  /**
   * SONAR-4412
   */
  @Test
  public void should_manage_user_permissions_using_ws() throws Exception {

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters firstUserParams = UserParameters.create().login("add_permission_user")
      .name("add_permission_user").password("password").passwordConfirmation("password");
    client.userClient().create(firstUserParams);
    UserParameters secondUserParams = UserParameters.create().login("add_remove_permission_user")
      .name("add_remove_permission_user").password("password").passwordConfirmation("password");
    client.userClient().create(secondUserParams);

    PermissionParameters firstUserPermissionsParams = PermissionParameters.create().user("add_permission_user").permission("profileadmin");
    client.permissionClient().addPermission(firstUserPermissionsParams);

    PermissionParameters secondUserProfileAdminParams = PermissionParameters.create().user("add_remove_permission_user").permission("profileadmin");
    client.permissionClient().addPermission(secondUserProfileAdminParams);
    PermissionParameters secondUserShareDashboardParams = PermissionParameters.create().user("add_remove_permission_user").permission("shareDashboard");
    client.permissionClient().addPermission(secondUserShareDashboardParams);
    client.permissionClient().removePermission(secondUserProfileAdminParams);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-users-permissions-using-ws",
      "/selenium/administration/permission-administration/manage-users-permission-with-ws.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4412
   */
  @Test
  public void should_manage_groups_permissions() throws Exception {

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    PermissionParameters usersGroupProfileAdminParams = PermissionParameters.create().group("sonar-users").permission("shareDashboard");
    client.permissionClient().addPermission(usersGroupProfileAdminParams);
    client.permissionClient().removePermission(usersGroupProfileAdminParams);

    PermissionParameters usersGroupShareDashboardParams = PermissionParameters.create().group("sonar-users").permission("profileadmin");
    client.permissionClient().addPermission(usersGroupShareDashboardParams);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-groups-permissions-using-ws",
      "/selenium/administration/permission-administration/manage-groups-permission-with-ws.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
