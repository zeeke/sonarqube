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
      "/selenium/administration/permission-templates/display-permission-template.html",
      "/selenium/administration/permission-templates/create-permission-template.html",
      "/selenium/administration/permission-templates/edit-permission-template.html",
      "/selenium/administration/permission-templates/delete-permission-template.html"
      )
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
