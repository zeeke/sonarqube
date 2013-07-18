/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class ProjectPermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUpProjects() {
    // To be replaced by the corresponding ws call to create the test template
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("create-permission-template-to-apply",
      "/selenium/administration/project-permissions/create-permission-template.html")
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
  public void should_bulk_apply_permission_template() throws Exception {

    SonarRunner sampleProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(sampleProject);

    SonarRunner sampleProjectWithTests = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample-with-tests"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(sampleProjectWithTests);

    SonarRunner sampleMultiModulesProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(sampleMultiModulesProject);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("bulk-apply-permission-template",
      "/selenium/administration/project-permissions/bulk-apply-permission-template.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4454
   */
  @Test
  public void should_apply_template_to_single_project() throws Exception {

    SonarRunner sampleProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(sampleProject);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("apply-permission-template-to-project",
      "/selenium/administration/project-permissions/apply-permission-template-to-project.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }
}
