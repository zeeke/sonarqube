/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

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

  private void checkFiles() {
    Resource one = getResource("com.sonarsource.it.projects.batch:custom-metric:src/main/java/One.java");
    assertThat(one).isNotNull();
    assertThat(one.getMeasureValue("custom")).isEqualTo(1.0);

    Resource two = getResource("com.sonarsource.it.projects.batch:custom-metric:src/main/java/Two.java");
    assertThat(two).isNotNull();
    assertThat(two.getMeasureValue("custom")).isEqualTo(2.0);
  }

  private void checkAggregation() {
    Resource pack = getResource("com.sonarsource.it.projects.batch:custom-metric:src/main/java");
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
