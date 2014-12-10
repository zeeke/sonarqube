/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class DifferentialPeriodsTest {

  @ClassRule
  public static Orchestrator orchestrator = HistoryTestSuite.ORCHESTRATOR;

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  /**
   * SONAR-4700
   */
  @Test
  public void not_display_periods_selection_dropdown_on_first_analysis() {
    SonarRunner analysis = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).withoutDynamicAnalysis();
    orchestrator.executeBuild(analysis);

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("not-display-periods-selection-dropdown-on-first-analysis",
      "/selenium/history/differential-periods/not-display-periods-selection-dropdown-on-dashboard.html"
      ).build());

    orchestrator.executeBuilds(analysis);

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("display-periods-selection-dropdown-after-first-analysis",
      "/selenium/history/differential-periods/display-periods-selection-dropdown-on-dashboard.html"
      ).build());
  }
}
