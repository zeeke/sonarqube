/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.fest.assertions.Delta;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class UnitTestTest {

  @ClassRule
  public static Orchestrator orchestrator = UiTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void testConditionsByLine() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/with-tests"))
      .setGoals("sonar:sonar");
    orchestrator.executeBuilds(newBuild("test/with-tests", false), analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:with-tests",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(1);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(66.7, Delta.delta(0.1));
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_success_density")).isEqualTo(100);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test",
      "/selenium/test/display-conditions-by-line.html").build();
    orchestrator.executeSelenese(selenese);
  }

  private MavenBuild newBuild(String projectPath, boolean ignoreTestFailure) {
    return MavenBuild.create()
      .setPom(ItUtils.locateProjectPom(projectPath))
      .setProperty("maven.test.failure.ignore", "" + ignoreTestFailure)
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent package");
  }

  /**
   * SONAR-4241
   */
  @Test
  public void should_not_display_branch_coverage_when_no_branch() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("exclusions/java-half-covered"))
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent package", "sonar:sonar");
    orchestrator.executeBuilds(analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.exclusions:java-half-covered",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(1);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0, Delta.delta(0.1));
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_success_density")).isEqualTo(100);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("no-branch-coverage",
      "/selenium/test/branch-coverage-hidden-if-no-branch.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See SONAR-3786
   */
  @Test
  public void shouldHaveTestFailures() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/test-failures"))
      .addGoal("sonar:sonar");
    orchestrator.executeBuilds(newBuild("test/test-failures", true), analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.test-failures:parent",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(6);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(33.3, Delta.delta(0.1));
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("test_success_density")).isEqualTo(50);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test-failures",
      "/selenium/test-failures/SONAR-1099-html-characters.html",
      "/selenium/test-failures/tests-viewer.html", // SONAR-3786
      "/selenium/test-failures/tests-viewer-with-skipped-test.html" // SONAR-3786
    ).build();
    orchestrator.executeSelenese(selenese);
  }

}
