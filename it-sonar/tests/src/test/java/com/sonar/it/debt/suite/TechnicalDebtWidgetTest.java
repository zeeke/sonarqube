/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class TechnicalDebtWidgetTest {

  @ClassRule
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.getDatabase().truncateInspectionTables();

    // need to execute the build twice in order to have history widgets
    // we made some exclusions to have variations in diff mode
    scanProject("2011-06-01", "**/a2/**");
    scanProject("2012-02-01", "");
  }

  /**
   * SONAR-4717
   */
  @Test
  public void technical_debt_in_issues_widget() {
    orchestrator.executeSelenese(Selenese.builder()
      .setHtmlTestsInClasspath("technical-debt-in-issues-widget",
        "/selenium/debt/widgets/technical-debt/should-have-correct-values.html",
        "/selenium/debt/widgets/technical-debt/should-open-remediationcost-on-drilldown-service.html",
        "/selenium/debt/widgets/technical-debt/display-differential-values.html",
        // SONAR-4717
        "/selenium/debt/widgets/technical-debt/is-in-issues-widget.html"
      ).build());
  }

  /**
   * SONAR-4718
   */
  @Test
  public void technical_debt_pyramid_widget() {
    orchestrator.executeSelenese(Selenese.builder()
      .setHtmlTestsInClasspath("technical-debt-pyramid-widget",
        "/selenium/debt/widgets/technical-debt-pyramid/should-have-correct-values.html",
        "/selenium/debt/widgets/technical-debt-pyramid/should-open-links-on-drilldown-service.html",
        "/selenium/debt/widgets/technical-debt-pyramid/display-differential-values.html"
      ).build());
  }

  private static void scanProject(String date, String excludes) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/with-many-rules.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.projectDate", date, "sonar.exclusions", excludes)
      .withoutDynamicAnalysis()
      .setProfile("with-many-rules");
    orchestrator.executeBuild(scan);
  }

}
