/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.util.VersionUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.system.Migration;
import org.sonar.wsclient.system.SystemClient;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;

public class SonarUpgradeTest {

  public static final String PROJECT_KEY = "org.apache.struts:struts-parent";

  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void test_upgrade_from_lts() {
    testDatabaseUpgrade("4.5.1", Orchestrator.builderEnv().getSonarVersion());
  }

  @Test
  public void test_upgrade_from_latest_release() {
    testDatabaseUpgrade("5.0", Orchestrator.builderEnv().getSonarVersion());
  }

  private void testDatabaseUpgrade(String fromVersion, String toVersion, BeforeUpgrade... tasks) {
    startServer(fromVersion, false);
    scanProject();
    int files = countFiles(PROJECT_KEY);
    assertThat(files).isGreaterThan(0);

    for (BeforeUpgrade task : tasks) {
      task.execute();
    }

    stopServer();
    startServer(toVersion, true);
    upgradeDatabase();

    assertThat(countFiles(PROJECT_KEY)).isEqualTo(files);
    browseWebapp();
    scanProject();
    assertThat(countFiles(PROJECT_KEY)).isEqualTo(files);
    browseWebapp();
  }

  private void browseWebapp() {
    testUrl("/");
    testUrl("/issues/index");
    testUrl("/dependencies/index");
    testUrl("/dashboard/index/org.apache.struts:struts-parent");
    testUrl("/components/index/org.apache.struts:struts-parent");
    testUrl("/issues/search");
    testUrl("/component/index?id=org.apache.struts%3Astruts-core%3Asrc%2Fmain%2Fjava%2Forg%2Fapache%2Fstruts%2Fchain%2Fcommands%2Fgeneric%2FWrappingLookupCommand.java");
    testUrl("/profiles");
  }

  private void upgradeDatabase() {
    SystemClient systemClient = SonarClient.create(orchestrator.getServer().getUrl()).systemClient();
    Migration migration = systemClient.migrate(/* timeout in ms */5L * 60 * 1000, 3L * 1000);
    assertThat(migration.operationalWebapp()).isTrue();
    assertThat(migration.status()).isEqualTo(Migration.Status.MIGRATION_SUCCEEDED);
  }

  private void startServer(String version, boolean keepDatabase) {
    OrchestratorBuilder builder = Orchestrator.builderEnv().setSonarVersion(version);
    builder.setOrchestratorProperty("orchestrator.keepDatabase", String.valueOf(keepDatabase));
    if (!keepDatabase) {
      builder.restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-5.1.xml"));
    }
    if (VersionUtils.isGreaterThanOrEqual(version, "4.2")) {
      builder.removeDistributedPlugins();
      builder.setOrchestratorProperty("javaVersion", "OLDEST_COMPATIBLE").addPlugin("java");
    } else if (VersionUtils.isGreaterThanOrEqual(version, "4.0")) {
      builder.removeDistributedPlugins();
      builder.setOrchestratorProperty("javaVersion", "RELEASE").addPlugin("java");
    }
    orchestrator = builder.build();
    orchestrator.start();
  }

  private void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  private void scanProject() {
    MavenBuild build = MavenBuild.create(new File("projects/struts-1.3.9-diet/pom.xml"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.profile", "sonar-way-5.1");
    // SONAR-3960 Cross project duplication is not supported on pgsql 8.3 before Sonar 3.3.2
    if (VersionUtils.isGreaterThanOrEqual(orchestrator.getServer().getVersion(), "3.3.2")
      || !orchestrator.getDatabase().getSonarProperties().get("sonar.jdbc.dialect").equals("postgresql")) {
      // SONAR-3340
      build.setProperty("sonar.cpd.cross_project", "true");
    }
    orchestrator.executeBuild(build);
  }

  private int countFiles(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "files")).getMeasureIntValue("files");
  }

  private void testUrl(String path) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(orchestrator.getServer().getUrl() + path);
      connection = (HttpURLConnection) url.openConnection();
      connection.connect();
      assertThat(connection.getResponseCode()).as("Fail to load " + path).isEqualTo(HttpURLConnection.HTTP_OK);

      // Error HTML pages generated by Ruby on Rails
      String content = IOUtils.toString(connection.getInputStream());
      assertThat(content).as("Fail to load " + path).doesNotContain("something went wrong");
      assertThat(content).as("Fail to load " + path).doesNotContain("The page you were looking for doesn't exist");
      assertThat(content).as("Fail to load " + path).doesNotContain("Unauthorized access");

    } catch (IOException e) {
      throw new IllegalStateException("Error with " + path, e);

    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private static interface BeforeUpgrade {
    void execute();
  }
}
