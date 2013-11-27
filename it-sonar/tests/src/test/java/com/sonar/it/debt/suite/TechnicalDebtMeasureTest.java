/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.Delta;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-4715
 */
public class TechnicalDebtMeasureTest {

  @ClassRule
  public static Orchestrator orchestrator = DebtTestSuite.ORCHESTRATOR;

  private static final Delta DELTA = Delta.delta(0.001);

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:com/sonar/it/samples/modules/a1/HelloA1.xoo";

  private static final String TECHNICAL_DEBT_MEASURE = "sqale_index";


  @BeforeClass
  public static void init() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/with-many-rules.xml"));
    orchestrator.executeBuild(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .withoutDynamicAnalysis()
      .setProfile("with-many-rules"));
  }

  @Test
  public void technical_debt_measures() {
    assertThat(getMeasure(PROJECT, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(1.456, DELTA);
    assertThat(getMeasure(MODULE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(0.647, DELTA);
    assertThat(getMeasure(SUB_MODULE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(0.239, DELTA);
    assertThat(getMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(0.062, DELTA);
    assertThat(getMeasure(FILE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(0.062, DELTA);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_project() {
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "PORTABILITY").getValue()).isEqualTo(0.0, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(0.008, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "SECURITY").getValue()).isEqualTo(1.239, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(0.108, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(0.1, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "RELIABILITY").getValue()).isEqualTo(0.0, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(0.008, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "TESTABILITY").getValue()).isEqualTo(0.0, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "API_ABUSE").getValue()).isEqualTo(1.239, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(0.1, DELTA);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(0.108, DELTA);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_modules() {
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "PORTABILITY").getValue()).isEqualTo(0.0, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(0.008, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "SECURITY").getValue()).isEqualTo(0.531, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(0.058, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(0.05, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "RELIABILITY").getValue()).isEqualTo(0.0, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(0.008, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "TESTABILITY").getValue()).isEqualTo(0.0, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "API_ABUSE").getValue()).isEqualTo(0.531, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(0.05, DELTA);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(0.058, DELTA);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_directory() {
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "PORTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(0.0041, Delta.delta(0.0001));
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "SECURITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(0.033, DELTA);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(0.025, DELTA);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "RELIABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(0.0041, Delta.delta(0.0001));
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "TESTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "API_ABUSE")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(0.025, DELTA);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(0.033, DELTA);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_file() {
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "PORTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(0.0041, Delta.delta(0.0001));
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "SECURITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(0.033, DELTA);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(0.025, DELTA);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "RELIABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(0.0041, Delta.delta(0.0001));
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "TESTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "API_ABUSE")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(0.025, DELTA);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(0.033, DELTA);
  }

  @Test
  @Ignore("can not load the measure: see SONAR-1874")
  public void technical_debt_measures_on_requirements_on_project() {
  }

  @Test
  public void not_save_zero_value_on_non_top_characteristics() throws Exception {
    String sqlRequest = "SELECT count(*) FROM project_measures WHERE characteristic_id IN (select id from characteristics where depth > 1) AND value = 0";
    assertThat(orchestrator.getDatabase().countSql(sqlRequest)).isEqualTo(0);
  }


  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }

  private Measure getCharacteristicMeasure(String resource, String metricKey, String characteristicKey) {
    Resource res = orchestrator.getServer().getWsClient().find(
      ResourceQuery.createForMetrics(resource, metricKey).setCharacteristics(characteristicKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }

}
