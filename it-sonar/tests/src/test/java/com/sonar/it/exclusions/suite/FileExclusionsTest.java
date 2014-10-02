/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.exclusions.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.fest.assertions.Delta;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FileExclusionsTest {
  static final String PROJECT = "exclusions";

  @ClassRule
  public static Orchestrator orchestrator = ExclusionsTestSuite.ORCHESTRATOR;

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void exclude_source_files() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.xoo",
      "sonar.exclusions", "**/*Exclude*.xoo,src/main/xoo/org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.xoo");

    Resource project = projectWithMetrics("ncloc", "files", "directories");

    assertThat(project.getMeasureIntValue("files")).isEqualTo(4);
    assertThat(project.getMeasureIntValue("ncloc")).isEqualTo(60);
    assertThat(project.getMeasureIntValue("directories")).isEqualTo(3);
  }

  /**
   * SONAR-2444 / SONAR-3758
   */
  @Test
  public void exclude_test_files() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.xoo",
      "sonar.exclusions", "**/*Exclude*.xoo,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.xoo");

    List<Resource> testFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("UTS").setDepth(-1));

    assertThat(testFiles).hasSize(2);
    assertThat(testFiles).onProperty("name").excludes("ClassTwoTest.xoo");
  }

  /**
   * SONAR-1896
   */
  @Test
  public void include_source_files() {
    scan(
      "sonar.dynamicAnalysis", "false",
      "sonar.inclusions", "**/*One.xoo,**/*Two.xoo");

    Resource project = projectWithMetrics("files");
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);

    List<Resource> sourceFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("FIL").setDepth(-1));

    assertThat(sourceFiles).hasSize(2);
    assertThat(sourceFiles).onProperty("name").containsOnly("ClassOne.xoo", "ClassTwo.xoo");
  }

  /**
   * SONAR-1896
   */
  @Test
  public void include_test_files() {
    scan("sonar.test.inclusions", "src/test/xoo/**/*One*.xoo,src/test/xoo/**/*Two*.xoo");

    Resource project = projectWithMetrics("tests");
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);

    List<Resource> testFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("UTS").setDepth(-1));

    assertThat(testFiles).hasSize(2);
    assertThat(testFiles).onProperty("name").containsOnly("ClassOneTest.xoo", "ClassTwoTest.xoo");
  }

  /**
   * SONAR-2760
   */
  @Test
  public void include_and_exclude_files_by_absolute_path() {
    scan(
      // includes everything except ClassOnDefaultPackage
      "sonar.inclusions", "file:**/src/main/xoo/org/**/*.xoo",

      // exclude ClassThree and ClassToExclude
      "sonar.exclusions", "file:**/src/main/xoo/org/**/packageToExclude/*.xoo,file:**/src/main/xoo/org/**/*Exclude.xoo");

    List<Resource> sourceFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("FIL").setDepth(-1));

    assertThat(sourceFiles).hasSize(4);
    assertThat(sourceFiles).onProperty("name").containsOnly("ClassOne.xoo", "ClassToIgnoreGlobally.xoo", "ClassTwo.xoo", "NoSonarComment.xoo");
  }

  @Test
  // TODO
  @Ignore("Xoo doesn't support coverage yet")
  public void exclude_files_from_coverage() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.xoo",
      "sonar.exclusions", "**/*Exclude*.xoo,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.xoo");

    Resource project = projectWithMetrics("tests", "test_failures", "test_success_density", "line_coverage");

    assertThat(project.getMeasureIntValue("tests")).isEqualTo(3);
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(1);
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(66.7, Delta.delta(0.1));
    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(17.1, Delta.delta(0.1));
  }

  @Test
  // TODO
  @Ignore("Xoo doesn't support duplication yet")
  public void exclude_files_from_duplication() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.xoo",
      "sonar.exclusions", "**/*Exclude*.xoo,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.xoo");

    Resource project = projectWithMetrics("duplicated_lines_density", "duplicated_lines", "duplicated_blocks", "duplicated_files");

    assertThat(project.getMeasureIntValue("duplicated_lines_density")).isZero();
    assertThat(project.getMeasureIntValue("duplicated_lines")).isZero();
    assertThat(project.getMeasureIntValue("duplicated_blocks")).isZero();
    assertThat(project.getMeasureIntValue("duplicated_files")).isZero();
  }

  static Resource projectWithMetrics(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys));
  }

  private void scan(String... properties) {
    SonarRunner build = SonarRunner
      .create(ItUtils.locateProjectDir("batch/exclusions"))
      .setProperties(properties);
    orchestrator.executeBuild(build);
  }
}
