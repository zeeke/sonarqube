/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.design;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DesignUITest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv().build();

  @BeforeClass
  public static void analyseProject() throws Exception {
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .addGoal("clean install")
      .withProperty("skipTests", "true")
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .build();


    orchestrator.executeBuild(build);
  }

  /**
   * SONAR-3114
   */
  @Test
  public void test_dependencies_tab() {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("design-pages-dependencies_tab",
        "/selenium/design/pages/dependencies_tab/should-have-correct-values.html",
        "/selenium/design/pages/dependencies_tab/should-open-links-in-popup.html").build();
    orchestrator.executeSelenese(selenese);
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
