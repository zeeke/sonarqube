/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ProjectServicesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin", "2.1.1"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1"))
    .build();

  @BeforeClass
  public static void inspectProject() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent verify", "sonar:sonar")
      .setProperty("sonar.language", "java")
      .setProfile("sonar-way-2.7");
    orchestrator.executeBuild(build);

    // this project is used by do-not-offer-coverage-choice-if-no-coverage.html
    build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.language", "java")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testComponents() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-components",
      "/selenium/ui/components/add_and_remove_column.html",
      "/selenium/ui/components/move_column_to_left_and_right.html",
      "/selenium/ui/components/remove_default_column_sort.html",
      "/selenium/ui/components/update_default_column_sort.html",
      // SONAR-3381
      "/selenium/ui/components/magnifying_glass_should_open_dashboard.html",
      // SONAR-3381
      "/selenium/ui/components/links_should_stay_on_component_page.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testMeasureDrilldown() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-measure-drilldown",
      // SONAR-3434
      "/selenium/ui/measure-drilldown/open-files-in-full-window-instead-of-below-drilldown.html",
      "/selenium/ui/measure-drilldown/unselect-filter.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testHotspots() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-hotspots",
      "/selenium/ui/hotspots/hotspot-metric-widget.html",

      // SONAR-3384
      "/selenium/ui/hotspots/hide-if-no-measures.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_source_viewers() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-source-viewers",
      "/selenium/ui/source-viewers/do-not-display-commits-option.html",
      "/selenium/ui/source-viewers/filter-violations-by-rule.html",
      "/selenium/ui/source-viewers/filter-violations-by-severity.html",
      "/selenium/ui/source-viewers/rule-severity-filter-is-not-set-by-default.html",
      "/selenium/ui/source-viewers/select-tab-by-metric.html",
      "/selenium/ui/source-viewers/SONAR-517-violations-drilldown-opens-violations-tab.html",
      "/selenium/ui/source-viewers/flag-resource-as-favourite.html" // SONAR-1650
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testComplexityWidget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-complexity-widget",
      "/selenium/ui/widgets/complexity-widget.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-3014
  @Test
  public void testDescriptionWidget() {
    long projectId = orchestrator.getServer().getWsClient().find(ResourceQuery.create("org.apache.struts:struts-parent")).getId();
    long qgateId = orchestrator.getServer().adminWsClient().qualityGateClient().show("SonarQube way").id();
    orchestrator.getServer().adminWsClient().qualityGateClient().selectProject(qgateId, projectId);
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-description-widget",
      "/selenium/ui/widgets/description-widget.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3651
   */
  @Test
  public void testCustomMeasuresWidget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-custom-measures-widget",
      "/selenium/ui/widgets/custom_measures/should-exclude-new-metrics.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See SONAR-830
   */
  @Test
  public void shouldFilterResourcesWhenNoMeasures() {
    Sonar client = orchestrator.getServer().getWsClient();
    ResourceQuery query = ResourceQuery.createForMetrics("org.apache.struts:struts-parent", "test_failures")
      .setAllDepths()
      .setScopes("FIL");
    List<Resource> resources = client.findAll(query);
    assertThat(resources).isEmpty();
  }

  /**
   * See SONAR-3862
   */
  @Test
  public void testAllProjectsPage() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-all-projects",
      "/selenium/ui/all-projects/should-display-all-projects-page.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-2342
   */
  @Test
  public void shouldDisplayRecentActivity() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-recent-activity",
      "/selenium/ui/recent-activity/should-not-add-libs-in-history.html",
      "/selenium/ui/recent-activity/should-clear-history-after-logout.html",
      "/selenium/ui/recent-activity/should-display-all-browsed-projects.html",
      "/selenium/ui/recent-activity/should-keep-history-order.html",
      "/selenium/ui/recent-activity/should-not-keep-files-in-history.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4580
   */
  @Test
  public void should_display_recent_activity_with_project_name_containing_a_quote() {
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true", "sonar.projectName", "Sample's");
    orchestrator.executeBuild(scan);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-recent-activity-with-quote-in-project-name",
      "/selenium/ui/recent-activity/should-display-project-with-quote-in-name.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See SONAR-37
   */
  @Test
  public void testComparisonService() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-comparison-service",
      "/selenium/ui/comparison/should-display-basic-set-of-metrics.html",
      "/selenium/ui/comparison/should-add-projects.html",
      "/selenium/ui/comparison/should-move-and-remove-projects.html",
      "/selenium/ui/comparison/should-add-metrics.html",
      "/selenium/ui/comparison/should-not-add-differential-metrics.html",
      "/selenium/ui/comparison/should-move-and-remove-metrics.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4552
   */
  @Test
  public void should_display_a_nice_error_when_requesting_unknown_project() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-services-display-nice-error-on-unknown-project",
      "/selenium/ui/should-display-nice-error-on-unknown-project.html").build();
    orchestrator.executeSelenese(selenese);
  }

}
