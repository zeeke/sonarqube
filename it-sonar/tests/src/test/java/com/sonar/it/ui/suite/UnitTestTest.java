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

  private MavenBuild newBuild(String projectPath, boolean ignoreTestFailure) {
    return MavenBuild.create()
      .setPom(ItUtils.locateProjectPom(projectPath))
      .setProperty("maven.test.failure.ignore", "" + ignoreTestFailure)
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent package");
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
  }

}
