/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.lang.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class SinceXDaysHistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = HistoryTestSuite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";

  @BeforeClass
  public static void analyseProjectWithHistory() {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over 30 days"

    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/history/one-issue-per-line-profile.xml"));

    // Execute a analysis in the past before since 30 days period -> 0 issue, 0 file
    analyzeProject("2013-01-01", "multi-modules-sample:module_b,multi-modules-sample:module_a");

    // Execute a analysis 20 days ago, after since 30 days period -> 16 issues, 1 file
    analyzeProject(getPastDate(20), "multi-modules-sample:module_b,multi-modules-sample:module_a:module_a2");

    // Execute a analysis 10 days ago, after since 30 days period -> 28 issues, 2 files
    analyzeProject(getPastDate(10), "multi-modules-sample:module_b");

    // Execute a analysis in the present with all modules -> 52 issues, 4 files
    analyzeProject();
  }

  @Test
  public void periods_are_well_defined() throws Exception {
    Resource project = getProject("files");

    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");

    assertThat(project.getPeriod2Mode()).isEqualTo("days");
    assertThat(project.getPeriod2Param()).isEqualTo("30");
  }

  @Test
  public void check_files_variation() throws Exception {
    checkMeasure("files", 2, 3);
  }

  @Test
  public void check_issues_variation() throws Exception {
    checkMeasure("violations", 24, 36);
  }

  @Test
  public void check_new_issues_measures() throws Exception {
    checkMeasure("new_violations", 24, 36);
  }

  private void checkMeasure(String measure, int variation1, int variation2){
    Resource project = getProject(measure);
    Measure newTechnicalDebt = project.getMeasure(measure);

    assertThat(newTechnicalDebt.getVariation1().intValue()).isEqualTo(variation1);
    assertThat(newTechnicalDebt.getVariation2().intValue()).isEqualTo(variation2);
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }

  private static void analyzeProject() {
    analyzeProject(null, null);
  }

  private static void analyzeProject(String date, String skippedModules) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/history/one-issue-per-line-profile.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProfile("one-issue-per-line")
      .setProperty("sonar.dynamicAnalysis", "false");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }

    if (skippedModules != null) {
      scan.setProperties("sonar.skippedModules", skippedModules);
    }

    orchestrator.executeBuild(scan);
  }

  private static String getPastDate(int nbPastDays){
    return new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.addDays(new Date(), nbPastDays * -1));
  }

}
