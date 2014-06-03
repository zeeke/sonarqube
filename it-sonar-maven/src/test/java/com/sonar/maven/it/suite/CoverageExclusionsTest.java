/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Delta.delta;
import static org.junit.Assume.assumeTrue;

public class CoverageExclusionsTest extends AbstractMavenTest {

  @Before
  public void resetData() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("4.0"));
    // Since 4.4 only reuse report mode is supported so no dependency on Maven
    assumeTrue(!orchestrator.getServer().version().isGreaterThanOrEquals("4.4"));

    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_compute_coverage_on_project() {
    scan("exclusions/java-half-covered");

    Resource project = getResourceForCoverage("com.sonarsource.it.exclusions:java-half-covered");
    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0, delta(0.1));
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_ignore_coverage_on_full_path() {
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      scan("exclusions/java-half-covered",
        "sonar.coverage.exclusions", "src/main/java/org/sonar/tests/halfcovered/UnCovered.java");
    } else {
      scan("exclusions/java-half-covered",
        "sonar.coverage.exclusions", "org/sonar/tests/halfcovered/UnCovered.java");
    }

    Resource project = getResourceForCoverage("com.sonarsource.it.exclusions:java-half-covered");
    assertThat(project.getMeasureValue("coverage")).isEqualTo(100.0);
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_ignore_coverage_on_pattern() {
    scan("exclusions/java-half-covered",
      "sonar.coverage.exclusions", "**/UnCovered*");

    Resource project = getResourceForCoverage("com.sonarsource.it.exclusions:java-half-covered");
    assertThat(project.getMeasureValue("coverage")).isEqualTo(100.0);
  }

  /**
   * SONAR-766
   */
  @Test
  public void should_not_have_coverage_at_all() {
    scan("exclusions/java-half-covered",
      "sonar.coverage.exclusions", "**/*");

    Resource project = getResourceForCoverage("com.sonarsource.it.exclusions:java-half-covered");
    assertThat(project).isNull();
  }

  protected void scan(String project, String... properties) {
    orchestrator.executeBuilds(newAnalysis(project, properties));
  }

  private MavenBuild newAnalysis(String projectPath, String... properties) {
    return MavenBuild.create()
      .setPom(ItUtils.locateProjectPom(projectPath))
      .setProperties(properties)
      .setGoals(cleanPackageSonarGoal());
  }

  private Resource getResourceForCoverage(String componentKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(componentKey, "coverage"));
  }
}
