/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.automated;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fest.assertions.Assertions.assertThat;

/**
 *
 *
 * AUTOMATED TEST
 * This test is executed by Jenkins in a dedicated stable environment
 *
 *
 */
public class SonarRunnerPerformanceTest {

  private static final Logger LOG = LoggerFactory.getLogger(SonarRunnerPerformanceTest.class);
  private static final double ACCEPTED_DURATION_VARIATION_IN_PERCENTS = 8.0;

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-3.6.xml"))
      .build();

  @Rule
  public TestName testName = new TestName();

  @BeforeClass
  public static void setUp() {
    // Execute sonar to generate jacoco reports
    MavenBuild build = MavenBuild.create(FileLocation.ofShared("it-sonar-performancing/tika-1.3/pom.xml").getFile())
        .setEnvironmentVariable("MAVEN_OPTS", "-Xmx512m -server")
        .setGoals("clean test-compile");
    orchestrator.executeBuild(build);

    // Set coverage per test profile as goal, waiting for orchestrator API to set maven profile
    build.setGoals("sonar:sonar -Pcoverage-per-test");
    orchestrator.executeBuild(build);

    // Execute this query in order for next analysis to not freeze (FIXME why does it freeze?)
    orchestrator.getDatabase().truncateInspectionTables();

    // Execute a first analysis to prevent any side effects with cache of plugin JAR files
    orchestrator.executeBuild(newSonarRunner("-Xmx512m -server", "sonar.dynamicAnalysis", "false", "sonar.profile", "empty"));
  }

  @Before
  public void cleanDatabase() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void most_basic_scan() {
    SonarRunner runner = newSonarRunner("-Xmx512m -server",
        "sonar.dynamicAnalysis", "false",
        "sonar.profile", "empty",
        "sonar.cpd.skip", "true"
        );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;
    assertDuration(duration, 60000L);
  }

  @Test
  public void scan_with_java_rules() {
    // checkstyle, pmd and squid but not findbugs
    SonarRunner runner = newSonarRunner("-Xmx512m -server",
        "sonar.dynamicAnalysis", "false",
        "sonar.profile", "sonar-way"
        );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;

    assertDuration(duration, 120000L);
  }

  @Test
  public void dry_run_scan_with_java_rules() {
    // checkstyle, pmd and squid but not findbugs
    SonarRunner runner = newSonarRunner("-Xmx512m -server",
        "sonar.dynamicAnalysis", "false",
        "sonar.profile", "sonar-way",
        "sonar.dryRun", "true"
        );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;

    assertDuration(duration, 92579L);
  }

  @Test
  @Ignore("To be enabled when most_basic_scan passes")
  public void scan_with_no_rule_and_coverage_per_test() {
    SonarRunner runner = newSonarRunner("-Xmx512m -server",
        "sonar.dynamicAnalysis", "reuseReports",
        "sonar.surefire.reportsPath", "target/surefire-reports",
        "sonar.jacoco.reportPath", "target/jacoco.exec",
        "sonar.profile", "empty"
        );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long duration = System.currentTimeMillis() - start;
    assertDuration(duration, 155600L);
  }

  @Test
  public void should_not_fail_with_limited_xmx_memory_and_no_coverage_per_test() {
    orchestrator.executeBuild(
        newSonarRunner("-Xmx80m -server -XX:-HeapDumpOnOutOfMemoryError", "sonar.dynamicAnalysis", "false")
        );
  }

  private static SonarRunner newSonarRunner(String sonarRunnerOpts, String... props) {
    return SonarRunner.create()
        .setProperties(props)
        .setEnvironmentVariable("SONAR_RUNNER_OPTS", sonarRunnerOpts)
        .setRunnerVersion("2.2.2")
        .setProjectDir(FileLocation.ofShared("it-sonar-performancing/tika-1.3").getFile());
  }

  private void assertDuration(long duration, long expectedDuration) {
    double variation = 100.0 * (0.0 + duration - expectedDuration) / expectedDuration;
    LOG.info("Test '" + testName.getMethodName() + " ' executed in " + duration + " ms (" + variation + "% from target)");
    assertThat(Math.abs(variation)).isLessThan(ACCEPTED_DURATION_VARIATION_IN_PERCENTS);
  }

}
