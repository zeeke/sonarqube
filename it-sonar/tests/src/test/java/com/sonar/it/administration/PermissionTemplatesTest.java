/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.administration;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Note: those tests should be integrated in the Administration test suite when the permission data integrity can be guaranteed
 * (i.e. with the use of a WS client to restore the default permissions)
 */
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
      "/selenium/administration/permission-templates/display-default-template.html",
      "/selenium/administration/permission-templates/create-template.html",
      "/selenium/administration/permission-templates/create-template-duplicate-name.html",
      "/selenium/administration/permission-templates/create-template-empty-name.html",
      "/selenium/administration/permission-templates/edit-template.html",
      "/selenium/administration/permission-templates/delete-template.html"
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
      "/selenium/administration/permission-templates/set-default-projects-template.html"
    )
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4465
   * SONAR-4466
   */
  @Test
  public void grant_permissions() {
    SonarRunner sonarRunnerBuild = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(sonarRunnerBuild);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("grant-permissions",
      "/selenium/administration/permission-templates/grant-default-project-groups.html",
      "/selenium/administration/permission-templates/grant-default-project-users.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }
}
