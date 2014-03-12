/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.permission.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

public class ProjectPermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4465
   */
  @Test
  public void manage_permissions_from_global_settings() {
    SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .withoutDynamicAnalysis();
    orchestrator.executeBuild(sonarRunnerBuild);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-permissions-from-global-settings",
      "/selenium/permission/project-permissions/display-permissions.html",
      "/selenium/permission/project-permissions/grant-project-users-from-global-administration.html",
      "/selenium/permission/project-permissions/grant-project-groups-from-global-administration.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4465
   */
  @Test
  @Ignore("Too many false positives with ui")
  public void manage_permissions_from_project_configuration() {
    String projectAdminUser = "with-admin-permission-on-project";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis();
      orchestrator.executeBuild(sonarRunnerBuild);

      // Create user having admin permission on previously analysed project
      adminClient.userClient().create(
        UserParameters.create().login(projectAdminUser).name(projectAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(projectAdminUser).component("sample").permission("admin"));

      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-permissions-from-project-configuration",
        "/selenium/permission/project-permissions/grant-project-users-from-project-configuration.html",
        "/selenium/permission/project-permissions/grant-project-groups-from-project-configuration.html"
      ).build();
      orchestrator.executeSelenese(selenese);

    } finally {
      adminClient.userClient().deactivate(projectAdminUser);
    }
  }

  /**
   * SONAR-4819
   */
  @Test
  public void grant_user_permission_on_a_project_as_an_project_admin_user_using_ws() {
    String projectAdminUser = "with-admin-permission-on-project";
    String projectUser = "with-user-permission-on-project";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).withoutDynamicAnalysis());

      // Create a user having admin permission on the previously analysed project
      adminClient.userClient().create(
        UserParameters.create().login(projectAdminUser).name(projectAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(projectAdminUser).component("sample").permission("admin"));

      // Create a user without permission
      adminClient.userClient().create(
        UserParameters.create().login(projectUser).name(projectUser).password("password").passwordConfirmation("password"));

      // Add user permission on project
      orchestrator.getServer().wsClient(projectAdminUser, "password").permissionClient().addPermission(
        PermissionParameters.create().user(projectUser).component("sample").permission("user"));

      // TODO verify permission has been granted when a WS to do that will be available
    } finally {
      adminClient.userClient().deactivate(projectAdminUser);
      adminClient.userClient().deactivate(projectUser);
    }
  }

  /**
   * SONAR-4819
   */
  @Test
  public void grant_user_permission_on_a_project_as_a_system_admin_user_using_ws() {
    String sysAdminUser = "with-system-admin-permission";
    String projectUser = "with-user-permission-on-project";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).withoutDynamicAnalysis());

      // Create a system admin user
      adminClient.userClient().create(
        UserParameters.create().login(sysAdminUser).name(sysAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(sysAdminUser).permission("admin"));

      // Create a user without permission
      adminClient.userClient().create(
        UserParameters.create().login(projectUser).name(projectUser).password("password").passwordConfirmation("password"));

      // Add user permission on project
      orchestrator.getServer().wsClient(sysAdminUser, "password").permissionClient().addPermission(
        PermissionParameters.create().user(projectUser).component("sample").permission("user"));

      // TODO verify permission has been granted when a WS to do that will be available
    } finally {
      adminClient.userClient().deactivate(sysAdminUser);
      adminClient.userClient().deactivate(projectUser);
    }
  }

  /**
   * SONAR-3383
   */
  @Test
  public void search_projects() {
    SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .withoutDynamicAnalysis();
    orchestrator.executeBuild(sonarRunnerBuild);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("search-projects",
      "/selenium/permission/project-permissions/search-projects.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }
}
