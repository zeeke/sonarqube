/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DashboardTest {

  @ClassRule
  public static Orchestrator orchestrator = UiTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void scanProject() {
    orchestrator.resetData();
    SonarRunner build = SonarRunner.create((ItUtils.locateProjectDir("shared/xoo-sample")))
      .setProperty("sonar.cpd.xoo.minimumLines", "3")
      .setProperty("sonar.cpd.xoo.minimumTokens", "6");
    orchestrator.executeBuild(build);
  }

  /**
   * SONAR-1929
   */
  @Test
  public void dashboard_extension() {
    seleniumSuite("dashboard_extension",
      "/selenium/ui/dashboard/dashboard_extension/dashboard-should-be-registered.html",
      "/selenium/ui/dashboard/dashboard_extension/test-location-of-widgets.html",
      "/selenium/ui/dashboard/dashboard_extension/test-name-l10n.html",

      // SSF-19
      "/selenium/ui/dashboard/dashboard_extension/xss.html");
  }

  /**
   * SONAR-3103
   */
  @Test
  public void share_dashboard() {
    seleniumSuite("share_dashboard",
      "/selenium/ui/dashboard/share_dashboard/follow-unfollow.html",
      "/selenium/ui/dashboard/share_dashboard/should-not-unshare-default-dashboard.html");
  }

  @Test
  public void configure_dashboard() {
    seleniumSuite("configure_dashboard",
      "/selenium/ui/dashboard/configure_dashboard/main_dashboard.html",
      "/selenium/ui/dashboard/configure_dashboard/filter_widgets.html",
      "/selenium/ui/dashboard/configure_dashboard/keep_filter_after_adding_widget.html");
  }

  @Test
  public void configure_widget() {
    seleniumSuite("configure_widget",
      "/selenium/ui/dashboard/configure_widget/add_project_widget_with_mandatory_properties.html");
  }

  @Test
  public void global_dashboard() {
    seleniumSuite("global_dashboard",
      // SONAR-3462
      "/selenium/ui/dashboard/global_dashboard/edit-global-dashboards.html",

      // SONAR-4630
      "/selenium/ui/dashboard/global_dashboard/create-global-dashboards-error.html",

      // SONAR-3462
      "/selenium/ui/dashboard/global_dashboard/edit-project-dashboards.html",

      // SONAR-3461
      "/selenium/ui/dashboard/global_dashboard/order-project-default-dashboards.html",

      // SONAR-3462
      "/selenium/ui/dashboard/global_dashboard/order-global-dashboard.html",

      // SONAR-1927 SONAR-3467
      "/selenium/ui/dashboard/global_dashboard/manage-global-dashboard.html",

      // SONAR-2073 SONAR-3459
      "/selenium/ui/dashboard/global_dashboard/filter-widget-admin.html",

      // SONAR-2073
      "/selenium/ui/dashboard/global_dashboard/filter-widget-anonymous.html",

      // SONAR-3460
      "/selenium/ui/dashboard/global_dashboard/global-admin-dashboards.html",

      // SONAR-3461
      "/selenium/ui/dashboard/global_dashboard/default-dashboards.html",

      // SONAR-3457 SONAR-3563
      "/selenium/ui/dashboard/global_dashboard/project-widget.html"

    );
  }

  @Test
  public void default_widgets() {
    seleniumSuite("default_widgets",
      "/selenium/ui/dashboard/default_widgets/welcome_widget.html",

      // SONAR-4448 TODO to be moved in another category
      "/selenium/ui/dashboard/default_widgets/documentation_and_comments_widget.html",

      // SONAR-4347 TODO to be moved in category duplications
      "/selenium/ui/dashboard/default_widgets/duplications_widget.html");
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }
}
