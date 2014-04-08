/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.TimeMachine;
import org.sonar.wsclient.services.TimeMachineCell;
import org.sonar.wsclient.services.TimeMachineQuery;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class HistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = HistoryTestSuite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:history";

  @BeforeClass
  public static void initialize() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/history/history-backup.xml"));
    analyzeProject("history/history-v1", "2010-10-19");
    analyzeProject("history/history-v2", "2010-11-13");
  }

  private static void analyzeProject(String path, String date) {
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom(path))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProperties("sonar.profile.java", "history")
      .setProperties("sonar.projectDate", date)
      );
  }

  @Test
  public void projectIsAnalyzed() {
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT)).getVersion()).isEqualTo("1.0-SNAPSHOT");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery(PROJECT)).getDate().getMonth()).isEqualTo(10); // November
  }

  @Test
  public void testHistoryOfViolations() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT, "blocker_violations", "critical_violations", "major_violations",
      "minor_violations", "info_violations");
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(query);
    assertThat(timemachine.getCells().length).isEqualTo(2);

    TimeMachineCell cell1 = timemachine.getCells()[0];
    TimeMachineCell cell2 = timemachine.getCells()[1];

    assertThat(cell1.getDate().getMonth()).isEqualTo(9);
    assertThat(cell1.getValues()).isEqualTo(new Object[] {0L, 0L, 3L, 4L, 0L});

    assertThat(cell2.getDate().getMonth()).isEqualTo(10);
    assertThat(cell2.getValues()).isEqualTo(new Object[] {0L, 0L, 5L, 4L, 0L});
  }

  @Test
  public void testHistoryOfMeasures() {
    TimeMachineQuery query = TimeMachineQuery.createForMetrics(PROJECT, "lines",// int
      "violations_density"// double
    );
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(query);
    assertThat(timemachine.getCells().length).isEqualTo(2);

    TimeMachineCell cell1 = timemachine.getCells()[0];
    TimeMachineCell cell2 = timemachine.getCells()[1];

    assertThat(cell1.getDate().getMonth()).isEqualTo(9);
    assertThat(cell1.getValues()).isEqualTo(new Object[] {32L, 38.1});

    assertThat(cell2.getDate().getMonth()).isEqualTo(10);
    assertThat(cell2.getValues()).isEqualTo(new Object[] {44L, 34.5});
  }

  // Specific cases for timemachine web service

  @Test
  public void unknownMetrics() {
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT, "notfound"));
    assertThat(timemachine.getCells().length).isEqualTo(0);

    timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT, "lines", "notfound"));
    assertThat(timemachine.getCells().length).isEqualTo(2);
    for (TimeMachineCell cell : timemachine.getCells()) {
      assertThat(cell.getValues().length).isEqualTo(1);
      assertThat(cell.getValues()[0]).isInstanceOf(Long.class);
    }

    timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT));
    assertThat(timemachine.getCells().length).isEqualTo(0);
  }

  @Test
  public void noDataForInterval() {
    Date now = new Date();
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics(PROJECT, "lines").setFrom(now).setTo(now));
    assertThat(timemachine.getCells().length).isEqualTo(0);
  }

  @Test
  public void unknownResource() {
    TimeMachine timemachine = orchestrator.getServer().getWsClient().find(TimeMachineQuery.createForMetrics("notfound:notfound", "lines"));
    assertThat(timemachine).isNull();
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

  @Test
  public void test_measure_variations() {
    Resource project = getProject("files", "ncloc", "violations");

    // period 1 : previous analysis
    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");
    assertThat(project.getPeriod1Date()).isNotNull();

    // variations from previous analysis
    assertThat(project.getMeasure("files").getVariation1()).isEqualTo(1.0);
    assertThat(project.getMeasure("ncloc").getVariation1()).isEqualTo(8.0);
    assertThat(project.getMeasure("violations").getVariation1()).isGreaterThan(0.0);
  }

  /**
   * SONAR-4962
   */
  @Test
  public void measure_variations_are_only_meaningful_when_includetrends() {
    String[] metricKeys = {"violations", "new_violations"};

    Resource projectWithTrends = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
    assertThat(projectWithTrends.getMeasure("violations")).isNotNull();
    assertThat(projectWithTrends.getMeasure("new_violations")).isNotNull();

    Resource projectWithoutTrends = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(false));
    assertThat(projectWithoutTrends.getMeasure("violations")).isNotNull();
    assertThat(projectWithoutTrends.getMeasure("new_violations")).isNull();
  }


  /**
   * SONAR-3046
   */
  @Test
  public void testPeriodInWidgetMostViolatedRules() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("history-widget-most-violated-rules",
      "/selenium/history/history-widget-most-violated-rules/display-variation.html",
      "/selenium/history/history-widget-most-violated-rules/select-rule-with-period.html",
      "/selenium/history/history-widget-most-violated-rules/open-drilldown-with-period.html"
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

  /**
   * SONAR-4334
   */
  @Test
  public void fail_if_project_date_is_older_than_latest_snapshot() {
    BuildResult result = orchestrator.executeBuildQuietly(MavenBuild.create(ItUtils.locateProjectPom("history/history-v1"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProperties("sonar.profile", "history")
      .setProperties("sonar.projectDate", "2000-10-19"));
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("'sonar.projectDate' property cannot be older than the date of the last known quality snapshot on this project. Value: '2000-10-19'. " +
      "Latest quality snapshot: ");
    assertThat(result.getLogs()).contains("This property may only be used to rebuild the past in a chronological order.");
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }
}
