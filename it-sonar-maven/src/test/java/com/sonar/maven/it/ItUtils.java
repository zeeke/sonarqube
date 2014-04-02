/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.MavenLocation;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
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
      .getParentFile(); // home/tests
  }

  public static MavenLocation xooPlugin() {
    return MavenLocation.create("com.sonarsource.xoo", "sonar-xoo-plugin", "1.0-SNAPSHOT");
  }

  public static File locateHome() {
    return home;
  }

  public static File locateProjectDir(String projectName) {
    return new File(locateHome(), "projects/" + projectName);
  }

  public static File locateProjectPom(String projectName) {
    return new File(locateProjectDir(projectName), "pom.xml");
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
  public static SonarClient newWsClient(Orchestrator o, String login, String password) {
    return o.getServer().wsClient(login, password);
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
    for (Iterator it = issues.iterator(); it.hasNext();) {
      JSONObject issue = (JSONObject) it.next();
      if (!onlyNews || (Boolean) issue.get("isNew")) {
        count++;
      }
    }
    return count;
  }
}
