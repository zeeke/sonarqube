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
public class DryRunScanPermissionTest {

  private final static String USER_LOGIN = "dryrunscan";

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;
  private static SonarClient client;

  @BeforeClass
  public static void createUser() {
    client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters userCreationParameters = UserParameters.create().login(USER_LOGIN).name(USER_LOGIN).password("thewhite").passwordConfirmation("thewhite");
    client.userClient().create(userCreationParameters);
  }

  @Before
  public void cleanup() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @After
  public void restorePermissionAndCleanup() {
    PermissionParameters permissionParameters = PermissionParameters.create().group("anyone").permission("dryRunScan");
    client.permissionClient().addPermission(permissionParameters);
  }

  @AfterClass
  public static void dropUser() {
    client.userClient().deactivate(USER_LOGIN);
  }

  @Test
  public void should_fail_if_no_dryrunscan_role() throws Exception {
    SonarRunner build = SonarRunner.create()
        .setProperty("sonar.login", USER_LOGIN)
        .setProperty("sonar.password", "thewhite")
        .setProperty("sonar.dryRun", "true")
        .setProjectDir(ItUtils.locateProjectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);
    // No error

    // Remove Anyone from dryrun permission
    PermissionParameters permissionParameters = PermissionParameters.create().group("anyone").permission("dryRunScan");
    client.permissionClient().removePermission(permissionParameters);

    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("You're not authorized to execute a dry run analysis. Please contact your SonarQube administrator.");
  }

  @Test
  public void should_fail_if_no_project_role() throws Exception {
    // Do a first analysis
    SonarRunner build = SonarRunner.create()
        .setProperty("sonar.login", USER_LOGIN)
        .setProperty("sonar.password", "thewhite")
        .setProjectDir(ItUtils.locateProjectDir("shared/xoo-sample"));
    orchestrator.executeBuild(build);
    // No error

    // Remove browse permission for groups Anyone on the project
    orchestrator.getServer().adminWsClient().permissionClient().removePermission(PermissionParameters.create().group("Anyone").permission("user").component("sample"));

    build.setProperty("sonar.dryRun", "true");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("You're not authorized to access to project 'Sample', please contact your SonarQube administrator");
  }

}
