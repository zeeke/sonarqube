/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.permission.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.*;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-4397
 */
public class ScanPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;


  private final static String USER_LOGIN = "scanperm";

  private static SonarClient client;

  @BeforeClass
  public static void createUser() {
    client = ItUtils.newWsClientForAdmin(orchestrator);

    ItUtils.newWsClientForAdmin(orchestrator).userClient().create(
      UserParameters.create().login(USER_LOGIN).name(USER_LOGIN).password("thewhite").passwordConfirmation("thewhite")
    );
  }

  @Before
  public void cleanup() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @After
  public void restorePermissions() {
    PermissionParameters permissionParameters = PermissionParameters.create().group("anyone").permission("scan");
    client.permissionClient().addPermission(permissionParameters);

    permissionParameters = PermissionParameters.create().group("anyone").permission("dryRunScan");
    client.permissionClient().addPermission(permissionParameters);
  }

  @AfterClass
  public static void dropUser() {
    client.userClient().deactivate(USER_LOGIN);
  }

  @Test
  public void should_fail_if_no_scan_role() throws Exception {
    SonarRunner build = SonarRunner.create()
        .setProperty("sonar.login", USER_LOGIN)
        .setProperty("sonar.password", "thewhite")
        .setProjectDir(ItUtils.locateProjectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);
    // No error

    // Remove Anyone from scan permission
    PermissionParameters permissionParameters = PermissionParameters.create().group("anyone").permission("scan");
    client.permissionClient().removePermission(permissionParameters);

    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains(
        "You're only authorized to execute a local (dry run) SonarQube analysis without pushing the results to the SonarQube server. Please contact your SonarQube administrator.");

    // Remove Anyone from dryrun permission
    permissionParameters = PermissionParameters.create().group("anyone").permission("dryRunScan");
    client.permissionClient().removePermission(permissionParameters);

    result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.");
  }

  @Test
  public void should_not_fail_if_no_project_role() throws Exception {
    // Do a first analysis
    SonarRunner build = SonarRunner.create()
        .setProperty("sonar.login", USER_LOGIN)
        .setProperty("sonar.password", "thewhite")
        .setProjectDir(ItUtils.locateProjectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);
    // No error

    // Remove browse permission for groups Anyone on the project
    orchestrator.getServer().adminWsClient().permissionClient().removePermission(PermissionParameters.create().group("Anyone").permission("user").component("sample"));

    BuildResult result = orchestrator.executeBuildQuietly(build);
    // No error
    assertThat(result.getStatus()).isEqualTo(0);
  }

}
