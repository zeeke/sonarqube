/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.administration.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;

public class PermissionTemplatesTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  /**
   * SONAR-4466
   */
  @Test
  public void should_manage_permission_templates() throws Exception {

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
  public void should_define_default_templates() throws Exception {

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-define-default-templates",
      "/selenium/administration/permission-templates/set-default-projects-template.html"
    )
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
