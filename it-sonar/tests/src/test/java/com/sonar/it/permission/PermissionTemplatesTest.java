/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.permission;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.user.UserParameters;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Note: those tests should be integrated in the Administration test suite when the permission data integrity can be guaranteed
 * (i.e. with the use of a WS client to restore the default permissions). TODO Test execution order matter while it should not.
 */
@Ignore("Too many false-positives")
public class PermissionTemplatesTest {

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4466
   * SONAR-4464
   */
  @Test
  public void manage_permission_templates() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-permission-templates",
      "/selenium/permission/permission-templates/display-default-template.html",
      "/selenium/permission/permission-templates/create-template.html",
      "/selenium/permission/permission-templates/create-template-duplicate-name.html",
      "/selenium/permission/permission-templates/create-template-empty-name.html",
      "/selenium/permission/permission-templates/edit-template.html",
      "/selenium/permission/permission-templates/delete-template.html"
      )
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4467
   */
  @Test
  public void define_default_templates() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-define-default-templates",
      "/selenium/permission/permission-templates/set-default-projects-template.html"
      )
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4465
   * SONAR-4466
   * Disabled because of false-positives -> require permissions WS (or loading spinner ?)
   */
  @Test
  @Ignore
  public void grant_permissions() {
    SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .withoutDynamicAnalysis();
    orchestrator.executeBuild(sonarRunnerBuild);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("grant-permissions",
      "/selenium/permission/permission-templates/grant-default-project-groups.html",
      "/selenium/permission/permission-templates/grant-default-project-users.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4535
   */
  @Test
  public void grant_permissions_based_on_key_pattern() {
    orchestrator.getServer().adminWsClient().userClient().create(UserParameters.create().login("userA").name("userA").password("password").passwordConfirmation("password"));
    try {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("create-template-with-key-pattern",
        "/selenium/permission/permission-templates/create-template-with-key-pattern.html"
        ).build();
      orchestrator.executeSelenese(selenese);

      SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperty("sonar.projectKey", "com.foo.MyProject")
        .withoutDynamicAnalysis();
      orchestrator.executeBuild(sonarRunnerBuild);

      selenese = Selenese.builder().setHtmlTestsInClasspath("verify-permissions-with-key-pattern",
        "/selenium/permission/permission-templates/verify-permissions-with-key-pattern.html"
        ).build();
      orchestrator.executeSelenese(selenese);

      // Provisionning
      selenese = Selenese.builder().setHtmlTestsInClasspath("provision-project-and-check-permissions",
        "/selenium/permission/permission-templates/provision-project-and-check-permissions.html"
        ).build();
      orchestrator.executeSelenese(selenese);
    } finally {
      orchestrator.getServer().adminWsClient().userClient().deactivate("userA");
    }
  }

  /**
   * SONAR-4535
   */
  @Test
  public void should_fail_when_project_key_match_several_patterns() {
    orchestrator.getServer().adminWsClient().userClient().create(UserParameters.create().login("userA").name("userA").password("password").passwordConfirmation("password"));
    try {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("create-2-templates-with-overlapping-key-pattern",
        "/selenium/permission/permission-templates/create-2-templates-with-overlapping-key-pattern.html"
        ).build();
      orchestrator.executeSelenese(selenese);

      SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperty("sonar.projectKey", "com.match.MyProject")
        .withoutDynamicAnalysis();
      BuildResult result = orchestrator.executeBuildQuietly(sonarRunnerBuild);
      assertThat(result.getStatus()).isNotEqualTo(0);
      assertThat(result.getLogs()).contains(
        "The \"com.match.MyProject\" key matches multiple permission templates: \"my-template-with-pattern-1\", \"my-template-with-pattern-2\". "
          + "A system administrator must update these templates so that only one of them matches the key.");
    } finally {
      orchestrator.getServer().adminWsClient().userClient().deactivate("userA");
    }
  }
}
