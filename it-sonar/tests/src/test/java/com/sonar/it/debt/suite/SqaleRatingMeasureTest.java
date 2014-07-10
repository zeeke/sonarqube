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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-4715
 */
public class SqaleRatingMeasureTest {

  @ClassRule
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

  @Before
  public void init() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void sqale_rating_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/with-many-rules.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProfile("with-many-rules"));

    assertThat(getMeasure(PROJECT, "sqale_rating").getIntValue()).isEqualTo(3);
    assertThat(getMeasure(PROJECT, "sqale_rating").getData()).isEqualTo("C");

    assertThat(getMeasure(MODULE, "sqale_rating").getValue()).isEqualTo(3);
    assertThat(getMeasure(MODULE, "sqale_rating").getData()).isEqualTo("C");

    assertThat(getMeasure(SUB_MODULE, "sqale_rating").getValue()).isEqualTo(3);
    assertThat(getMeasure(SUB_MODULE, "sqale_rating").getData()).isEqualTo("C");

    assertThat(getMeasure(DIRECTORY, "sqale_rating").getValue()).isEqualTo(1);
    assertThat(getMeasure(DIRECTORY, "sqale_rating").getData()).isEqualTo("A");

    assertThat(getMeasure(FILE, "sqale_rating").getValue()).isEqualTo(1);
    assertThat(getMeasure(FILE, "sqale_rating").getData()).isEqualTo("A");
  }

  @Test
  public void use_development_cost_parameter() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProfile("one-issue-per-line"));

    Measure rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperty("sonar.technicalDebt.developmentCost", "2")
        .setProfile("one-issue-per-line"));

    rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(4);
    assertThat(rating.getData()).isEqualTo("D");
  }

  @Test
  public void use_size_metric_parameter() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProfile("one-issue-per-line"));

    Measure rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperty("sonar.technicalDebt.sizeMetric", "complexity")
        .setProfile("one-issue-per-line"));

    rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(2);
    assertThat(rating.getData()).isEqualTo("B");
  }

  @Test
  public void use_language_specific_parameters() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProfile("one-issue-per-line"));

    Measure rating = getMeasure(PROJECT, "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProperties("languageSpecificParameters", "0", "languageSpecificParameters.0.language", "xoo",
          "languageSpecificParameters.0.man_days", "1", "languageSpecificParameters.0.size_metric", "ncloc")
        .setProfile("one-issue-per-line"));

    rating = getMeasure(PROJECT, "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(5);
    assertThat(rating.getData()).isEqualTo("E");
  }

  @Test
  public void use_rating_grid_parameter() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProfile("one-issue-per-line"));

    Measure rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperty("ratingGrid", "0.001,0.005,0.010,0.015")
        .setProfile("one-issue-per-line"));

    rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(5);
    assertThat(rating.getData()).isEqualTo("E");
  }

  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }

}
