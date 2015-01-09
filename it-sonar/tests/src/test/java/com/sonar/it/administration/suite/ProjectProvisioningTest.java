/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;

public class ProjectProvisioningTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  /**
   * SONAR-3871
   * SONAR-4711
   * SONAR-4724
   * SONAR-4712
   */
  @Test
  public void should_allow_provisioning_from_admin_ui() {
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("provisioning-from-admin-ui",
        "/selenium/administration/provisioning/open-provisioning-page-as-admin.html",
        "/selenium/administration/provisioning/provision-project-and-check-dashboard.html",
        "/selenium/administration/provisioning/provisioned-project-can-be-configured.html",
        "/selenium/administration/provisioning/provisioned-project-is-not-a-ghost.html",
        "/selenium/administration/provisioning/provisioned-project-appears-in-search.html",
        "/selenium/administration/provisioning/provisioning-form-validates-fields.html",
        "/selenium/administration/provisioning/provisioned-project-can-be-deleted.html"
        ).build());
  }

}
