/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class DryRunTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
    File dryRunCache = new File(new File(orchestrator.getServer().getHome(), "temp"), "dryRun");
    FileUtils.deleteQuietly(dryRunCache);
  }

  @Test
  public void test_dry_run() {
    BuildResult result = scan("shared/xoo-sample",
      "sonar.dryRun", "true");

    // Analysis is not persisted in database
    Resource project = getResource("com.sonarsource.it.samples:simple-sample");
    assertThat(project).isNull();
    assertThat(result.getLogs()).contains("Dry run");
    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL");
  }

  @Test
  public void should_fail_if_plugin_access_secured_properties() {
    // Test access from task (ie BatchSettings)
    SonarRunner runner = configureRunner("shared/xoo-sample",
      "sonar.dryRun", "true",
      "accessSecuredFromTask", "true");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in local (dry run) SonarQube analysis. "
      + "The SonarQube plugin which requires this property must be deactivated in dry run mode.");

    // Test access from sensor (ie ModuleSettings)
    runner = configureRunner("shared/xoo-sample",
      "sonar.dryRun", "true",
      "accessSecuredFromSensor", "true");
    result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getLogs()).contains("Access to the secured property 'foo.bar.secured' is not possible in local (dry run) SonarQube analysis. "
      + "The SonarQube plugin which requires this property must be deactivated in dry run mode.");
  }

  // SONAR-4488
  @Test
  @Ignore("1 second is enough to generate dry run DB on Jenkins so the test fail")
  public void should_fail_if_dryrun_timeout_is_too_short() {
    // Test access from task (ie BatchSettings)
    SonarRunner runner = configureRunner("shared/xoo-sample",
      "sonar.dryRun", "true",
      "sonar.dryRun.readTimeout", "1");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains(
      "DryRun database read timed out after 1000 ms. You can try to increase read timeout with property -Dsonar.dryRun.readTimeout (in seconds)");
  }

  // SONAR-4468
  @Test
  public void test_build_breaker_with_dry_run() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/batch/DryRunTest/SimpleAlertProfile.xml"));
    SonarRunner runner = configureRunner("shared/xoo-sample",
      "sonar.dryRun", "true")
      .setProfile("SimpleAlertProfile");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("[BUILD BREAKER] Lines of code > 5");
    assertThat(result.getLogs()).contains("Alert thresholds have been hit (1 times)");
  }

  // SONAR-4594
  @Test
  public void test_build_breaker_with_dry_run_and_differential_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/batch/DryRunTest/VariationAlertProfile.xml"));

    // First analysis
    SonarRunner runner = configureRunner("shared/xoo-sample");
    BuildResult result = orchestrator.executeBuild(runner);

    // Second analysis
    runner = configureRunner("batch/dry-run-build-breaker",
      "sonar.dryRun", "true")
      .setProfile("VariationAlertProfile");
    result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("[BUILD BREAKER] Lines of code variation > 2 since previous analysis");
    assertThat(result.getLogs()).contains("Alert thresholds have been hit (1 times)");
  }

  // SONAR-4594
  @Test
  public void test_build_breaker_with_dry_run_and_differential_measures_last_version() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/batch/DryRunTest/VariationSinceLastVersionAlertProfile.xml"));

    // First analysis 1.0-SNAPSHOT
    SonarRunner runner = configureRunner("shared/xoo-sample");
    orchestrator.executeBuild(runner);

    // Second analysis 2.0-SNAPSHOT
    runner = configureRunner("batch/dry-run-build-breaker",
      "sonar.dryRun", "true",
      "sonar.projectVersion", "2.0-SNAPSHOT")
      .setProfile("VariationSinceLastVersionAlertProfile");
    BuildResult result = orchestrator.executeBuildQuietly(runner);

    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("[BUILD BREAKER] Lines of code variation > 2 since previous version (1.0-SNAPSHOT)");
    assertThat(result.getLogs()).contains("Alert thresholds have been hit (1 times)");
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines"));
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuild(runner);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      .setRunnerVersion("2.2.2")
      .setProperties(props);
    return runner;
  }

}
