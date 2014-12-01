/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.permission.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.project.Project;
import org.sonar.wsclient.user.UserParameters;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProvisioningPermissionTest {

  private static final String PASSWORD = "password";

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;

  private static final String ADMIN_WITH_PROVISIONING = "admin-with-provisioning";
  private static final String ADMIN_WITHOUT_PROVISIONING = "admin-without-provisioning";
  private static final String USER_WITH_PROVISIONING = "user-with-provisioning";
  private static final String USER_WITHOUT_PROVISIONING = "user-without-provisioning";

  @BeforeClass
  public static void init() {
    SonarClient client = orchestrator.getServer().adminWsClient();

    client.userClient().create(UserParameters.create().login(ADMIN_WITH_PROVISIONING).name(ADMIN_WITH_PROVISIONING)
      .password(PASSWORD).passwordConfirmation(PASSWORD));
    client.permissionClient().addPermission(PermissionParameters.create().user(ADMIN_WITH_PROVISIONING).permission("admin"));
    client.permissionClient().addPermission(PermissionParameters.create().user(ADMIN_WITH_PROVISIONING).permission("provisioning"));

    client.userClient().create(UserParameters.create().login(ADMIN_WITHOUT_PROVISIONING).name(ADMIN_WITHOUT_PROVISIONING)
      .password(PASSWORD).passwordConfirmation(PASSWORD));
    client.permissionClient().addPermission(PermissionParameters.create().user(ADMIN_WITHOUT_PROVISIONING).permission("admin"));
    client.permissionClient().removePermission(PermissionParameters.create().user(ADMIN_WITHOUT_PROVISIONING).permission("provisioning"));

    client.userClient().create(UserParameters.create().login(USER_WITH_PROVISIONING).name(USER_WITH_PROVISIONING)
      .password(PASSWORD).passwordConfirmation(PASSWORD));
    client.permissionClient().addPermission(PermissionParameters.create().user(USER_WITH_PROVISIONING).permission("provisioning"));

    client.userClient().create(UserParameters.create().login(USER_WITHOUT_PROVISIONING).name(USER_WITHOUT_PROVISIONING)
      .password(PASSWORD).passwordConfirmation(PASSWORD));
    client.permissionClient().removePermission(PermissionParameters.create().user(USER_WITHOUT_PROVISIONING).permission("provisioning"));
  }

  @AfterClass
  public static void deactivateUsers() {
    SonarClient client = orchestrator.getServer().adminWsClient();
    client.userClient().deactivate(ADMIN_WITH_PROVISIONING);
    client.userClient().deactivate(ADMIN_WITHOUT_PROVISIONING);
    client.userClient().deactivate(USER_WITH_PROVISIONING);
    client.userClient().deactivate(USER_WITHOUT_PROVISIONING);
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_not_see_provisioning_section() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should-not-see-provisioning-section",
      "/selenium/permission/provisioning/provisioning-page-hidden.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_see_provisioning_section() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should-see-provisioning-section",
      "/selenium/permission/provisioning/provisioning-page-allowed.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_be_allowed_on_ws_with_permission() {
    final String newKey = "new-project";
    final String newName = "New Project";

    SonarClient client = orchestrator.getServer().wsClient(USER_WITH_PROVISIONING, PASSWORD);

    Project created = client.projectClient().create(NewProject.create().key(newKey).name(newName));

    assertThat(created).isNotNull();
    assertThat(created.key()).isEqualTo(newKey);
    assertThat(created.name()).isEqualTo(newName);
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_not_be_allowed_on_ws_without_permission() {
    SonarClient client = orchestrator.getServer().wsClient(USER_WITHOUT_PROVISIONING, PASSWORD);

    try {
      client.projectClient().create(NewProject.create().key("new-project").name("New Project"));
      fail();
    } catch (HttpException e) {
      assertThat(e.getMessage()).contains("401");
    }
  }
}
