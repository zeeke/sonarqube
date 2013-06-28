/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.cartography;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class ResourcesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.javascript", "sonar-javascript-plugin", "1.1"))
      .addPlugin(ItUtils.xooPlugin())
      .build();

  @After
  public void cleanup() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void should_resources_be_filtered_by_language() {
    scanProject("javascript/javascript-maven-project");
    scanProject("shared/sample");

    assertThat(orchestrator.getServer().getWsClient().findAll(new ResourceQuery())).hasSize(2);
    assertThat(orchestrator.getServer().getWsClient().findAll(new ResourceQuery().setLanguages("java"))).hasSize(1);
    assertThat(orchestrator.getServer().getWsClient().findAll(new ResourceQuery().setLanguages("js"))).hasSize(1);
    assertThat(orchestrator.getServer().getWsClient().findAll(new ResourceQuery().setLanguages("java,js"))).hasSize(2);
    assertThat(orchestrator.getServer().getWsClient().findAll(new ResourceQuery().setLanguages("unknown"))).isEmpty();
  }

  // SONAR-4235
  @Test
  @Ignore("wait for Goldeneye")
  public void test_resource_creation_date() {
    long before = new Date().getTime();
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")));
    long after = new Date().getTime();
    Resource xooSample = orchestrator.getServer().getWsClient().find(new ResourceQuery().setLanguages("xoo"));
    // assertThat(xooSample.getCreationDate().getTime()).isGreaterThan(before).isLessThan(after);
  }

  private void scanProject(String project) {
    MavenBuild maven = MavenBuild.create(ItUtils.locateProjectPom(project))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(maven);
  }
}
