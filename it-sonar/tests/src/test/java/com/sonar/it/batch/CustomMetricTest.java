/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("Need a milestone of SQ API because custom-metric-plugin rely on new API")
public class CustomMetricTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("custom-metric-plugin"))
    .build();

  @Test
  public void custom_formula_should_be_executed() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/custom-metric"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");

    orchestrator.executeBuild(build);

    checkFiles();
    checkAggregation();
  }

  // SONAR-4066
  @Test
  public void useful_error_when_unable_to_save_measure() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/custom-metric"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.it.failingMeasure", "true");

    BuildResult result = orchestrator.executeBuildQuietly(build);

    assertThat(result.getLogs()).contains("Unable to save measure for metric [custom] on resource [/src/main/java/Break.java]");
  }

  private void checkFiles() {
    Resource one = getResource("com.sonarsource.it.projects.batch:custom-metric:[default].One");
    assertThat(one).isNotNull();
    assertThat(one.getMeasureValue("custom")).isEqualTo(1.0);

    Resource two = getResource("com.sonarsource.it.projects.batch:custom-metric:[default].Two");
    assertThat(two).isNotNull();
    assertThat(two.getMeasureValue("custom")).isEqualTo(2.0);
  }

  private void checkAggregation() {
    Resource pack = getResource("com.sonarsource.it.projects.batch:custom-metric:[default]");
    assertThat(pack).isNotNull();
    double packageValue = 2.5 * 1.0 + 2.5 * 2.0;
    assertThat(pack.getMeasureValue("custom")).isEqualTo(packageValue);

    Resource project = getResource("com.sonarsource.it.projects.batch:custom-metric");
    assertThat(project).isNotNull();
    assertThat(project.getMeasureValue("custom")).isEqualTo((2.5 * packageValue));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines", "custom"));
  }
}
