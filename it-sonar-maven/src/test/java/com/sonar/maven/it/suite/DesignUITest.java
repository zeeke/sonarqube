/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.Test;

public class DesignUITest extends AbstractMavenTest {

  @BeforeClass
  public static void analyseProject() throws Exception {
    orchestrator.getDatabase().truncateInspectionTables();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .setGoals(cleanInstallSonarGoal())
      .setProperty("skipTests", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void test_dependencies_page() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("design-page-dependencies_page",
      "/selenium/design/pages/dependencies_page/no_results.html",
      "/selenium/design/pages/dependencies_page/search_subprojects.html",
      "/selenium/design/pages/dependencies_page/standard_usage.html",
      "/selenium/design/pages/dependencies_page/too_short_search.html",
      "/selenium/design/pages/dependencies_page/should_not_display_project_name_in_breadcrumbs.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_libraries_page() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("design-page-libraries",
      "/selenium/design/pages/libraries/display_tests.html",
      "/selenium/design/pages/libraries/keyword_filter.html",
      "/selenium/design/pages/libraries/module_libraries.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_design() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("design-on-project",
      "/selenium/design/design_from_tools_menu.html",
      "/selenium/design/design_from_drilldown.html",
      "/selenium/design/design_in_popup.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

}
