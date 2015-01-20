/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch2;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

/**
 * One language per module
 *
 */
public class OldMultiLanguageTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.javascript", "sonar-javascript-plugin", "2.0"))
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.python", "sonar-python-plugin", "1.3"))
    .addPlugin(ItUtils.javaPlugin())
    .build();

  @After
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void test_maven_inspection() {
    MavenBuild build = MavenBuild
      .create(ItUtils.locateProjectPom("batch/multi-languages"))
      .setCleanSonarGoals()
      .setDebugLogs(true);
    BuildResult result = orchestrator.executeBuild(build);

    // SONAR-4515
    assertThat(result.getLogs()).contains("Available languages:");
    assertThat(result.getLogs()).contains("JavaScript => \"js\"");
    assertThat(result.getLogs()).contains("Python => \"py\"");
    assertThat(result.getLogs()).contains("Java => \"java\"");

    // modules
    Resource javaModule = getResource("com.sonarsource.it.projects.batch.multi-languages:java-module", "files");
    Resource jsModule = getResource("com.sonarsource.it.projects.batch.multi-languages:javascript-module", "files");
    Resource pyModule = getResource("com.sonarsource.it.projects.batch.multi-languages:python-module", "files");
    verifyModule(javaModule, 1);
    verifyModule(jsModule, 1);
    verifyModule(pyModule, 2);

    // project
    Resource project = getResource("com.sonarsource.it.projects.batch.multi-languages:multi-languages", "files");
    verifyProject(project);
  }

  @Test
  public void test_sonar_runner_inspection() {
    SonarRunner build = SonarRunner.create().setProjectDir(ItUtils.locateProjectDir("batch/multi-languages"));
    orchestrator.executeBuild(build);

    // modules
    Resource javaModule = getResource("multi-languages:java-module", "files");
    Resource jsModule = getResource("multi-languages:javascript-module", "files");
    Resource pyModule = getResource("multi-languages:python-module", "files");
    verifyModule(javaModule, 1);
    verifyModule(jsModule, 1);
    verifyModule(pyModule, 2);

    // project
    Resource project = getResource("multi-languages", "files");
    verifyProject(project);

  }

  private void verifyModule(Resource module, int files) {
    assertThat(module.getMeasureIntValue("files")).isEqualTo(files);
    assertThat(module.getLanguage()).isNull();
  }

  private void verifyProject(Resource project) {
    verifyModule(project, 4);
  }

  private Resource getResource(String resourceKey, String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
  }
}
