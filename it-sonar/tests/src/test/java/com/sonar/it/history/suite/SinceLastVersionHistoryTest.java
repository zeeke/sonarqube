/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class SinceLastVersionHistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = HistoryTestSuite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";

  /**
   * SONAR-2496
   */
  @Test
  public void test_since_last_version_period() {
    orchestrator.getDatabase().truncateInspectionTables();
    analyzeProject("0.9", "**/*2.java");
    analyzeProject("1.0-SNAPSHOT", null);
    analyzeProject("1.0-SNAPSHOT", null);

    Resource project = getProject("files");
    Measure measure = project.getMeasure("files");

    // There are 4 files
    assertThat(measure.getValue()).isEqualTo(4);

    // nothing changed in the previous analysis
    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");
    assertThat(measure.getVariation1()).isEqualTo(0);

    // but 2 files were added since the first analysis which was version 0.9
    assertThat(project.getPeriod4Mode()).isEqualTo("previous_version");
    assertThat(project.getPeriod4Param()).isEqualTo("0.9");
    assertThat(measure.getVariation4()).isEqualTo(2);
  }

  private static void analyzeProject(String version, String exclusions) {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
        .setCleanSonarGoals()
        .setProperties("sonar.dynamicAnalysis", "false")
        .setProperties("sonar.projectVersion", version)
        .setProperties("sonar.timemachine.period4", "previous_version");
    if (exclusions != null) {
      build.setProperties("sonar.exclusions", exclusions);
    }

    orchestrator.executeBuild(build);
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }
}
