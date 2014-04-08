/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.After;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.system.Migration;
import org.sonar.wsclient.system.SystemClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class ResourceKeyMigrationTest {

  public static final String PROJECT_KEY = "sample-with-tests";

  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  // SONAR-3024
  @Test
  public void testResourceKeyMigration() {
    startServer("4.1", false);
    assumeTrue(ItUtils.isUpgradableDatabase(orchestrator));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
    scanProject();
    int initialIssues = countIssues(PROJECT_KEY);
    assertThat(initialIssues).isGreaterThan(0);

    stopServer();
    startServer(Orchestrator.builderEnv().getSonarVersion(), true);
    upgradeDatabase();

    assertThat(countIssues(PROJECT_KEY)).isEqualTo(initialIssues);

    BuildResult result = scanProject();
    assertThat(result.getLogs()).contains("Update component keys");
    assertThat(result.getLogs()).contains("Component sample-with-tests:sample/Sample.xoo changed to sample-with-tests:src/main/xoo/sample/Sample.xoo");
    assertThat(result.getLogs()).contains("Component sample-with-tests:sample/SampleTest.xoo changed to sample-with-tests:src/test/xoo/sample/SampleTest.xoo");
    assertThat(result.getLogs()).contains(
      "Directory with key sample-with-tests:sample matches both sample-with-tests:src/main/xoo/sample and sample-with-tests:src/test/xoo/sample. First match is arbitrary chosen.");
    assertThat(result.getLogs()).contains("Component sample-with-tests:sample changed to sample-with-tests:src/main/xoo/sample");
    assertThat(countIssues(PROJECT_KEY)).isEqualTo(initialIssues);
    assertThat(countNewIssues(PROJECT_KEY)).isEqualTo(0);

    result = scanProject();
    assertThat(result.getLogs()).excludes("Update component keys");
    assertThat(countIssues(PROJECT_KEY)).isEqualTo(initialIssues);
    assertThat(countNewIssues(PROJECT_KEY)).isEqualTo(0);
  }

  private int countIssues(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "violations")).getMeasureIntValue("violations");
  }

  private int countNewIssues(String key) {
    Measure measure = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "new_violations").setIncludeTrends(true))
      .getMeasures().get(0);
    return measure.getVariation1().intValue();
  }

  private void upgradeDatabase() {
    SystemClient systemClient = SonarClient.create(orchestrator.getServer().getUrl()).systemClient();
    Migration migration = systemClient.migrate(/* timeout in ms */5L * 60 * 1000, 3L * 1000);
    assertThat(migration.operationalWebapp()).isTrue();
    assertThat(migration.status()).isEqualTo(Migration.Status.MIGRATION_SUCCEEDED);
  }

  private BuildResult scanProject() {
    SonarRunner build = SonarRunner.create()
      .setProjectDir(ItUtils.locateProjectDir("shared/xoo-sample-with-tests"))
      .setProfile("one-issue-per-line");
    return orchestrator.executeBuild(build);
  }

  private void startServer(String version, boolean keepDatabase) {
    OrchestratorBuilder builder = Orchestrator.builderEnv().setSonarVersion(version);
    builder.setOrchestratorProperty("orchestrator.keepDatabase", String.valueOf(keepDatabase))
      .addPlugin(ItUtils.xooPlugin());
    orchestrator = builder.build();
    orchestrator.start();
  }

  private void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }
}
