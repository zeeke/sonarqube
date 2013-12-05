/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.permission;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
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
  @Ignore("To be fixed by SONAR-4827")
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
   */
  @Test
  @Ignore("To be fixed by SONAR-4827")
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
}
