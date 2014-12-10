/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class HistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = HistoryTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void initialize() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/history/history-backup.xml"));
    analyzeProject("history/history-v1", "2010-10-19");
    analyzeProject("history/history-v2", "2010-11-13");
  }

  private static void analyzeProject(String path, String date) {
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom(path))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProfile("history")
      .setProperties("sonar.projectDate", date));
  }

  @Test
  public void testTimeLineWidget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("history-timeline-widget",
      "/selenium/history/history-timeline-widget/timeline-widget.html",
      // SONAR-3561
      "/selenium/history/history-timeline-widget/should-display-even-if-one-missing-metric.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testTimeMachineWidget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("history-timemachine-widget",
      "/selenium/history/history-timemachine-widget/time-machine-widget.html",
      // SONAR-3354 & SONAR-3353
      "/selenium/history/history-timemachine-widget/should-display-empty-table-if-no-measure.html",
      // SONAR-3650
      "/selenium/history/history-timemachine-widget/should-exclude-new-metrics.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-2911
   */
  @Test
  public void testComparisonPageBetweenProjectVersions() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("comparison-page",
      "/selenium/history/comparison/should-compare-project-versions.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }
}
