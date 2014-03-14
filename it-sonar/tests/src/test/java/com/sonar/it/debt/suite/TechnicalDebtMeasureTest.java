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
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

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
    assertThat(getMeasure(PROJECT, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(436);
    assertThat(getMeasure(MODULE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(222);
    assertThat(getMeasure(SUB_MODULE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(113);
    assertThat(getMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(28);
    assertThat(getMeasure(FILE, TECHNICAL_DEBT_MEASURE).getValue()).isEqualTo(28);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_project() {
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "PORTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "SECURITY").getValue()).isEqualTo(340);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(52);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(40);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "RELIABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "TESTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "REUSABILITY").getValue()).isEqualTo(0);

    // sub characteristics
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "API_ABUSE").getValue()).isEqualTo(340);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(40);
    assertThat(getCharacteristicMeasure(PROJECT, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(52);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_modules() {
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "PORTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "SECURITY").getValue()).isEqualTo(170);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(28);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(20);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "RELIABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(4);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "TESTABILITY").getValue()).isEqualTo(0);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "REUSABILITY").getValue()).isEqualTo(0);

    // sub characteristics
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "API_ABUSE").getValue()).isEqualTo(170);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(20);
    assertThat(getCharacteristicMeasure(MODULE, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(28);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_directory() {
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "PORTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "SECURITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(16);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "RELIABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "TESTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "API_ABUSE")).isNull();
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(DIRECTORY, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(16);
  }

  @Test
  public void technical_debt_measures_on_characteristics_on_file() {
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "PORTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "MAINTAINABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "SECURITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "EFFICIENCY").getValue()).isEqualTo(16);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "RELIABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "READABILITY").getValue()).isEqualTo(2);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "TESTABILITY")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "REUSABILITY")).isNull();

    // sub characteristics
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "API_ABUSE")).isNull();
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "ARCHITECTURE_CHANGEABILITY").getValue()).isEqualTo(10);
    assertThat(getCharacteristicMeasure(FILE, TECHNICAL_DEBT_MEASURE, "MEMORY_EFFICIENCY").getValue()).isEqualTo(16);
  }

  @Test
  @Ignore("can not load the measure: see SONAR-1874")
  public void technical_debt_measures_on_requirements_on_project() {
  }

  @Test
  public void not_save_zero_value_on_non_top_characteristics() throws Exception {
    String sqlRequest = "SELECT count(*) FROM project_measures WHERE characteristic_id IN (select id from characteristics where parent_id IS NOT NULL) AND value = 0";
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
