/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.scm.suite;

import com.google.common.base.Throwables;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.util.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({SvnTest.class, GitTest.class})
public class ScmTestSuite {

  public static final File PROJECTS_DIR = new File("target/projects");
  public static final File SOURCES_DIR = new File("scm-sources");

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin("java")
    .setContext("/")

    .build();

  public static void unzip(String zipName) {
    try {
      FileUtils.forceMkdir(PROJECTS_DIR);
      ZipUtils.unzip(new File(SOURCES_DIR, zipName), PROJECTS_DIR);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static void runSonar(String projectName, String... keyValues) {
    File pom = new File(project(projectName), "pom.xml");

    MavenBuild install = MavenBuild.create(pom).setGoals("clean install");
    MavenBuild sonar = MavenBuild.create(pom).setGoals("sonar:sonar");
    sonar.setProperties(keyValues);
    ORCHESTRATOR.executeBuilds(install, sonar);
  }

  public static Measure measure(String key, String metricKey) {
    Resource resource = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, metricKey).setIncludeTrends(true));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  public static File project(String name) {
    return new File(PROJECTS_DIR, name);
  }
}
