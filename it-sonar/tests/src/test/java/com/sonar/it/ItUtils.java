/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.apache.commons.io.FileUtils;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.util.Properties;

public final class ItUtils {

  private ItUtils() {
  }

  private static final File home;

  static {
    File testResources = FileUtils.toFile(ItUtils.class.getResource("/ItUtilsLocator.txt"));
    home = testResources // home/tests/src/tests/resources
      .getParentFile() // home/tests/src/tests
      .getParentFile() // home/tests/src
      .getParentFile() // home/tests
      .getParentFile(); // home
  }

  public static MavenLocation xooPlugin() {
    return MavenLocation.create("com.sonarsource.xoo", "sonar-xoo-plugin", "1.0-SNAPSHOT");
  }

  public static File locateHome() {
    return home;
  }

  public static File locatePluginDir(String pluginDirname) {
    return new File(locateHome(), "plugins/" + pluginDirname);
  }

  public static File locateProjectDir(String projectName) {
    return new File(locateHome(), "tests/projects/" + projectName);
  }

  public static File locateProjectPom(String projectName) {
    return new File(locateProjectDir(projectName), "pom.xml");
  }

  public static FileLocation locateTestPlugin(String artifactId) {
    return locateTestPlugin(locateTestPluginDir(artifactId), artifactId);
  }

  public static FileLocation locateTestPlugin(String subDirectory, String artifactId, String version) {
    return locateTestPlugin(locateTestPluginDir(subDirectory), artifactId, version);
  }

  public static FileLocation locateTestPlugin(String subDirectory, String artifactId) {
    return locateTestPlugin(locateTestPluginDir(subDirectory), artifactId);
  }

  private static FileLocation locateTestPlugin(File pluginDir, String artifactId) {
    return locateTestPlugin(pluginDir, artifactId, "1.0-SNAPSHOT");
  }

  private static FileLocation locateTestPlugin(File pluginDir, String artifactId, String version) {
    File pluginJar = new File(pluginDir, "target/" + artifactId + "-" + version + ".jar");
    if (!pluginJar.exists()) {
      throw new IllegalArgumentException("Jar file of test plugin does not exist: " + pluginJar);
    }
    return FileLocation.of(pluginJar);
  }

  private static File locateTestPluginDir(String artifactId) {
    File pluginDir = locatePluginDir(artifactId);
    if (!pluginDir.exists()) {
      throw new IllegalArgumentException("Directory of test plugin does not exist: " + pluginDir);
    }
    return pluginDir;
  }

  public static Measure getMeasure(Orchestrator orchestrator, String resourceKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  public static MavenBuild inspectWithoutTests(Orchestrator orchestrator, String path, Properties extraProperties) {
    MavenBuild.Builder builder = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom(path))
      .addGoal("clean verify")
      .withProperty("skipTests", "true")
      .addSonarGoal()
      .withDynamicAnalysis(false);
    if (extraProperties != null) {
      builder.withProperties(extraProperties);
    }
    MavenBuild build = builder.build();
    orchestrator.executeBuild(build);
    return build;
  }

  public static SonarClient newWsClient(Orchestrator o, String login, String password) {
    return SonarClient.builder().login(login).password(password).url(o.getServer().getUrl()).build();
  }

  public static SonarClient newWsClientForAdmin(Orchestrator o) {
    return newWsClient(o, "admin", "admin");
  }

  public static SonarClient newWsClientForAnonymous(Orchestrator o) {
    return newWsClient(o, null, null);
  }

  public static String sanitizeTimezones(String s) {
    return s.replaceAll("[\\+\\-]\\d\\d\\d\\d", "+0000");
  }
}
