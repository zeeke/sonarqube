/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.fest.assertions.Delta;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FileExclusionsTest {
  private static final String PROJECT = "com.sonarsource.it.projects.batch:exclusions";

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Before
  public void truncateData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void should_exclude_source_files() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.java",
      "sonar.exclusions", "**/*Exclude*.java,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.java"
    );

    Resource project = projectWithMetrics("ncloc", "classes", "packages", "functions");

    assertThat(project.getMeasureIntValue("ncloc")).isEqualTo(60);
    assertThat(project.getMeasureIntValue("classes")).isEqualTo(4);
    assertThat(project.getMeasureIntValue("packages")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("functions")).isEqualTo(8);
  }

  /**
   * SONAR-2444 / SONAR-3758
   */
  @Test
  public void should_exclude_test_files() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.java",
      "sonar.exclusions", "**/*Exclude*.java,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.java"
    );

    List<Resource> testFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("UTS").setDepth(-1));

    assertThat(testFiles).hasSize(2);
    assertThat(testFiles).onProperty("name").excludes("ClassTwoTest", "package-info.java");
  }

  /**
   * SONAR-1896
   */
  @Test
  public void should_include_source_files() {
    scan(
      "sonar.dynamicAnalysis", "false",
      "sonar.inclusions", "**/*One.java,**/*Two.java"
    );

    Resource project = projectWithMetrics("files");
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);

    List<Resource> sourceFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("CLA").setDepth(-1));

    assertThat(sourceFiles).hasSize(2);
    assertThat(sourceFiles).onProperty("name").containsOnly("ClassOne", "ClassTwo");
  }

  /**
   * SONAR-1896
   */
  @Test
  public void should_include_test_files() {
    scan("sonar.test.inclusions", "**/*One*.java,**/*Two*.java");

    Resource project = projectWithMetrics("tests");
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);

    List<Resource> testFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("UTS").setDepth(-1));

    assertThat(testFiles).hasSize(2);
    assertThat(testFiles).onProperty("name").containsOnly("ClassOneTest", "ClassTwoTest");
  }

  /**
   * SONAR-2760
   */
  @Test
  public void should_include_and_exclude_files_by_absolute_path() {
    scan(
      // includes everything except ClassOnDefaultPackage
      "sonar.inclusions", "file:**/src/main/java/org/**/*.java",

      // exclude ClassThree and ClassToExclude
      "sonar.exclusions", "file:**/src/main/java/org/**/packageToExclude/*.java,file:**/src/main/java/org/**/*Exclude.java"
    );

    List<Resource> sourceFiles = orchestrator.getServer().getWsClient()
      .findAll(new ResourceQuery(PROJECT).setQualifiers("CLA").setDepth(-1));

    assertThat(sourceFiles).hasSize(4);
    assertThat(sourceFiles).onProperty("name").containsOnly("ClassOne", "ClassToIgnoreGlobally", "ClassTwo", "NoSonarComment");
  }

  @Test
  public void should_exclude_files_from_coverage() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.java",
      "sonar.exclusions", "**/*Exclude*.java,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.java"
    );

    Resource project = projectWithMetrics("tests", "test_failures", "test_success_density", "line_coverage");

    assertThat(project.getMeasureIntValue("tests")).isEqualTo(3);
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(1);
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(66.7, Delta.delta(0.1));
    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(17.1, Delta.delta(0.1));
  }

  @Test
  public void should_exclude_files_from_duplication() {
    scan(
      "sonar.global.exclusions", "**/*Ignore*.java",
      "sonar.exclusions", "**/*Exclude*.java,org/sonar/tests/packageToExclude/**",
      "sonar.test.exclusions", "**/ClassTwoTest.java"
    );

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
    MavenBuild build = MavenBuild
      .create(ItUtils.locateProjectPom("batch/exclusions"))
      .setCleanPackageSonarGoals()
      .setProperties(properties)
      .setProperty("maven.test.failure.ignore", "true");
    orchestrator.executeBuild(build);
  }
}
