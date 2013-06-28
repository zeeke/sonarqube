/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DashboardTest {
  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("dashboard-plugin"))
      .build();

  @BeforeClass
  public static void inspectProject() {
    MavenBuild inspection = MavenBuild.builder()
        .setPom(ItUtils.locateProjectPom("shared/sample"))
        .addGoal("clean install")
        .addSonarGoal()
        .withDynamicAnalysis(false)
        .build();

    orchestrator.executeBuild(inspection);
  }

  /**
   * SONAR-1929
   */
  @Test
  public void test_dashboard_extension() {
    seleniumSuite("dashboard-extension",
        "/selenium/ui/dashboard-extension/dashboard-should-be-registered.html",
        "/selenium/ui/dashboard-extension/test-location-of-widgets.html",
        "/selenium/ui/dashboard-extension/test-name-l10n.html");
  }

  /**
   * SONAR-3103
   */
  @Test
  public void test_dashboard_sharing() {
    seleniumSuite("dashboard-sharing",
        "/selenium/ui/dashboard-sharing/follow-unfollow.html",
        "/selenium/ui/dashboard-sharing/should-not-unshare-default-dashboard.html");
  }

  @Test
  public void test_configuration_of_dashboards() {
    seleniumSuite("dashboard-configuration",
        "/selenium/ui/dashboard-configuration/main_dashboard.html",
        "/selenium/ui/dashboard-configuration/filter_widgets.html",
        "/selenium/ui/dashboard-configuration/keep_filter_after_adding_widget.html");
  }

  @Test
  public void test_global_dashboards() {
    seleniumSuite("dashboard-global",
        "/selenium/ui/dashboard-global/edit-global-dashboards.html", // SONAR-3462
        "/selenium/ui/dashboard-global/edit-project-dashboards.html", // SONAR-3462
        "/selenium/ui/dashboard-global/order-project-default-dashboards.html", // SONAR-3461
        "/selenium/ui/dashboard-global/order-global-dashboard.html", // SONAR-3462
        "/selenium/ui/dashboard-global/manage-global-dashboard.html", // SONAR-1927 and SONAR-3467
        "/selenium/ui/dashboard-global/filter-widget-admin.html", // SONAR-2073 and SONAR-3459
        "/selenium/ui/dashboard-global/filter-widget-anonymous.html", // SONAR-2073
        "/selenium/ui/dashboard-global/global-anonymous-dashboards.html", // SONAR-3460
        "/selenium/ui/dashboard-global/global-admin-dashboards.html", // SONAR-3460
        "/selenium/ui/dashboard-global/default-dashboards.html", // SONAR-3461
        "/selenium/ui/dashboard-global/project-widget.html" // SONAR-3457 && SONAR-3563
    );
  }

  @Test
  public void test_welcome_widget() {
    seleniumSuite("welcome_widget","/selenium/ui/widgets/welcome_widget.html"
    );
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }
}
