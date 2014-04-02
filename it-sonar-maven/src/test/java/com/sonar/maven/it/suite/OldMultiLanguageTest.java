/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

/**
 * One language per module
 *
 */
public class OldMultiLanguageTest extends AbstractMavenTest {

  @Before
  public void cleanDatabase() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void test_maven_inspection() {
    MavenBuild build = MavenBuild
      .create(ItUtils.locateProjectPom("batch/multi-languages"))
      .setProperty("sonar.profile.java", "empty")
      .setProperty("sonar.profile.js", "empty")
      .setProperty("sonar.profile.py", "empty")
      .setGoals(cleanSonarGoal())
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
