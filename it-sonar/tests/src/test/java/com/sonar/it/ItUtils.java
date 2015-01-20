/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.*;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;

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

  public static MavenLocation javaPlugin() {
    return MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-java-plugin", "2.8");
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
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom(path))
      .setGoals("clean verify", "sonar:sonar")
      .setProperty("skipTests", "true")
      .setProperties("sonar.dynamicAnalysis", "false");
    if (extraProperties != null) {
      for (Map.Entry<Object, Object> entry : extraProperties.entrySet()) {
        build.setProperty(entry.getKey().toString(), entry.getValue().toString());
      }
    }
    orchestrator.executeBuild(build);
    return build;
  }

  /**
   * @deprecated
   */
  public static SonarClient newWsClientForAdmin(Orchestrator o) {
    return o.getServer().adminWsClient();
  }

  /**
   * @deprecated
   */
  public static SonarClient newWsClientForAnonymous(Orchestrator o) {
    return o.getServer().wsClient();
  }

  public static String sanitizeTimezones(String s) {
    return s.replaceAll("[\\+\\-]\\d\\d\\d\\d", "+0000");
  }

  public static Date toDate(String sDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static void executeUpdate(Orchestrator orchestrator, String sql) {
    Connection connection = null;
    PreparedStatement statement;
    try {
      connection = orchestrator.getDatabase().openConnection();
      statement = connection.prepareStatement(sql);
      int result = statement.executeUpdate();
      if (result != 1) {
        throw new RuntimeException();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      orchestrator.getDatabase().closeQuietly(connection);
    }
  }

  public static JSONObject getJSONReport(BuildResult result) {
    Pattern pattern = Pattern.compile("Export results to (.*?).json");
    Matcher m = pattern.matcher(result.getLogs());
    if (m.find()) {
      String s = m.group(1);
      File path = new File(s + ".json");
      assertThat(path).exists();
      try {
        return (JSONObject) JSONValue.parse(FileUtils.readFileToString(path));
      } catch (IOException e) {
        throw new RuntimeException("Unable to read JSON report", e);
      }
    }
    fail("Unable to locate json report");
    return null;
  }

  public static int countIssuesInJsonReport(BuildResult result, boolean onlyNews) {
    JSONObject obj = getJSONReport(result);
    JSONArray issues = (JSONArray) obj.get("issues");
    int count = 0;
    for (Object issue : issues) {
      JSONObject jsonIssue = (JSONObject) issue;
      if (!onlyNews || (Boolean) jsonIssue.get("isNew")) {
        count++;
      }
    }
    return count;
  }

  public static boolean isUpgradableDatabase(Orchestrator orchestrator) {
    return !"h2".equals(orchestrator.getDatabase().getClient().getDialect());
  }

  public static void setServerProperty(Orchestrator orch, String key, @Nullable String value) {
    if (value == null) {
      orch.getServer().getAdminWsClient().delete(new PropertyDeleteQuery(key));
    } else {
      orch.getServer().getAdminWsClient().update(new PropertyUpdateQuery().setKey(key).setValue(value));
    }
  }
}
