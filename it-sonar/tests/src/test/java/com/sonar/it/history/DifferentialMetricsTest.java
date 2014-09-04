/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DifferentialMetricsTest {

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/history/DifferentialMetricsTest/one-issue-per-line-profile.xml"))
    .build();

  @BeforeClass
  public static void scanProject() {
    SonarRunner firstBuild = SonarRunner
      .create(ItUtils.locateProjectDir("shared/xoo-history-v1"), "sonar.profile", "one-issue-per-line-profile", "sonar.projectDate", "2013-01-05");
    orchestrator.executeBuild(firstBuild);

    SonarRunner secondBuild = SonarRunner
      .create(ItUtils.locateProjectDir("shared/xoo-history-v2"), "sonar.profile", "one-issue-per-line-profile", "sonar.projectDate", "2013-01-07");
    orchestrator.executeBuild(secondBuild);
  }

  /**
   * SONAR-3858
   */
  @Test
  public void should_display_modified_files_in_differential_drilldown() throws Exception {
    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("differential-drilldown",
        "/selenium/history/differential-metrics/display-added-files.html",
        "/selenium/history/differential-metrics/display-modified-files.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }
}
