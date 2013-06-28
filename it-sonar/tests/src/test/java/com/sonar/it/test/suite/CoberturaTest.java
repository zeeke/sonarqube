/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.test.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class CoberturaTest {

  @ClassRule
  public static Orchestrator orchestrator = TestTestSuite.ORCHESTRATOR;

  public static final String PROJECT_KEY = "org.apache.struts:struts-parent";
  public static final String MODULE_KEY = "org.apache.struts:struts-core";

  @BeforeClass
  public static void executeCoberturaMavenPlugin() {
    orchestrator.getDatabase().truncateInspectionTables();
    MavenBuild analysis = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .withDynamicAnalysis(true)
      .withProperty("sonar.java.coveragePlugin", "cobertura")
      .addSonarGoal()
      .build();
    orchestrator.executeBuilds(newBuild("shared/struts-1.3.9-diet"), analysis);
  }

  @Test
  public void testProjectCoverage() {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "coverage"));
    assertThat(project.getMeasureValue("coverage"), is(25.1));
  }

  @Test
  public void testModuleCoverage() {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(MODULE_KEY, "coverage"));
    assertThat(project.getMeasureValue("coverage"), is(36.3));
  }

  @Test
  public void shouldExecuteSurefire() {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(MODULE_KEY, "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time"));
    assertThat(project.getMeasureValue("test_success_density"), is(100.0));
    assertThat(project.getMeasureIntValue("test_failures"), is(0));
    assertThat(project.getMeasureIntValue("test_errors"), is(0));
    assertThat(project.getMeasureIntValue("tests"), is(195));
    assertThat(project.getMeasureIntValue("skipped_tests"), is(0));
    assertThat(project.getMeasureIntValue("test_execution_time"), greaterThan(10));
  }

  private static MavenBuild newBuild(String projectPath) {
    return MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom(projectPath))
      .addGoal("clean install")
      .withProperty("skipTests", "true")
      .build();
  }
}
