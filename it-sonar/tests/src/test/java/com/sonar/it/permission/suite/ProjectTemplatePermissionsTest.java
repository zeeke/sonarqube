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

@Ignore("Too many false-positives")
public class ProjectTemplatePermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUpProjects() {
    // To be replaced by the corresponding ws call to create the test template
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("create-permission-template",
      "/selenium/permission/project-permissions/create-permission-template.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4453
   */
  @Test
  public void bulk_apply_permission_template() throws Exception {
    String sysAdminUser = "with-system-admin-permission";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      // Create user having system admin permission
      adminClient.userClient().create(
        UserParameters.create().login(sysAdminUser).name(sysAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(sysAdminUser).permission("admin"));

      SonarRunner sampleProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis();
      orchestrator.executeBuild(sampleProject);

      SonarRunner sampleProjectWithTests = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample-with-tests"))
        .withoutDynamicAnalysis();
      orchestrator.executeBuild(sampleProjectWithTests);

      SonarRunner sampleMultiModulesProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .withoutDynamicAnalysis();
      orchestrator.executeBuild(sampleMultiModulesProject);

      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("bulk-apply-permission-template",
        "/selenium/permission/project-permissions/bulk-apply-permission-template.html")
        .build();
      orchestrator.executeSelenese(selenese);

    } finally {
      adminClient.userClient().deactivate(sysAdminUser);
    }
  }

  /**
   * SONAR-4454
   */
  @Test
  public void apply_template_to_single_project_from_global_settings() throws Exception {
    String sysAdminUser = "with-system-admin-permission";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      // Create user having system admin permission
      adminClient.userClient().create(
        UserParameters.create().login(sysAdminUser).name(sysAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(sysAdminUser).permission("admin"));

      SonarRunner sampleProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis();
      orchestrator.executeBuild(sampleProject);

      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("apply-permission-template-to-project-from-global-settings",
        "/selenium/permission/project-permissions/apply-permission-template-to-project-from-global-settings.html")
        .build();
      orchestrator.executeSelenese(selenese);

    } finally {
      adminClient.userClient().deactivate(sysAdminUser);
    }
  }

  /**
   * SONAR-4521
   * SONAR-4819
   */
  @Test
  public void apply_template_to_single_project_from_project_configuration() throws Exception {
    String projectAdminUser = "with-admin-permission-on-project";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();

    try {
      orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .withoutDynamicAnalysis());

      // Create user having admin permission on previously analysed project
      adminClient.userClient().create(
        UserParameters.create().login(projectAdminUser).name(projectAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(projectAdminUser).component("sample").permission("admin"));

      orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("apply-permission-template-to-project-from-project--settings",
        "/selenium/permission/project-permissions/apply-permission-template-to-project-from-project-settings.html")
        .build());
    } finally {
      adminClient.userClient().deactivate(projectAdminUser);
    }
  }

}
