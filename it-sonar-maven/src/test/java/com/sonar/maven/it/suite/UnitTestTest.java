/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.fest.assertions.Delta;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class UnitTestTest extends AbstractMavenTest {

  @Before
  public void deleteData() {
    // Since 4.4 only reuse report mode is supported so no dependency on Maven
    assumeTrue(!orchestrator.getServer().version().isGreaterThanOrEquals("4.4"));

    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void testDynamicAnalysis() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/with-tests"))
      .setProperty("sonar.profile.java", "empty")
      .addGoal(sonarGoal());
    orchestrator.executeBuilds(newBuild("test/with-tests"), analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:with-tests",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(1);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(66.7, Delta.delta(0.1));
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_success_density")).isEqualTo(100);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);

    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test",
        "/selenium/test/display-conditions-by-line.html").build();
      orchestrator.executeSelenese(selenese);
    }
  }

  private MavenBuild newBuild(String projectPath) {
    return MavenBuild.create()
      .setPom(ItUtils.locateProjectPom(projectPath))
      .setGoals("clean", "install")
      .setProperty("skipTests", "true");
  }

  /**
   * SONAR-4241
   */
  @Test
  public void should_not_display_branch_coverage_when_no_branch() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("exclusions/java-half-covered"))
      .setProperty("sonar.profile.java", "empty")
      .setGoals(cleanPackageSonarGoal());
    orchestrator.executeBuilds(analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.exclusions:java-half-covered",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(1);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0, Delta.delta(0.1));
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_success_density")).isEqualTo(100);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);

    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("no-branch-coverage",
        "/selenium/test/branch-coverage-hidden-if-no-branch.html").build();
      orchestrator.executeSelenese(selenese);
    }
  }

  /**
   * See SONAR-2371
   */
  @Test
  public void testDynamicAnalysisWithNoTests() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/no-tests"))
      .setProperty("sonar.profile.java", "empty")
      .addGoal(sonarGoal());
    orchestrator.executeBuilds(newBuild("test/no-tests"), analysis);

    // check project measures
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:no-tests",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("coverage")).isEqualTo(0);
    assertThat(project.getMeasure("test_failures")).isNull();
    assertThat(project.getMeasure("test_errors")).isNull();
    assertThat(project.getMeasure("test_success_density")).isNull();
    assertThat(project.getMeasure("skipped_tests")).isNull();
    assertThat(project.getMeasure("test_execution_time")).isNull();

    // check package measures
    // TODO Godin: are we really sure about this behavior? maybe we should save zero tests on packages too?
    Resource packagee = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(getPackageKey(),
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(packagee.getMeasureIntValue("tests")).isNull();
    assertThat(packagee.getMeasureIntValue("coverage")).isEqualTo(0);
    assertThat(packagee.getMeasure("test_failures")).isNull();
    assertThat(packagee.getMeasure("test_errors")).isNull();
    assertThat(packagee.getMeasure("test_success_density")).isNull();
    assertThat(packagee.getMeasure("skipped_tests")).isNull();
    assertThat(packagee.getMeasure("test_execution_time")).isNull();
  }

  private String getPackageKey() {
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      return "com.sonarsource.it.samples:no-tests:src/main/java/org/sonar/tests";
    } else {
      return "com.sonarsource.it.samples:no-tests:org.sonar.tests";
    }
  }

  @Test
  public void shouldNotHaveTestMeasuresOnStaticAnalysis() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/with-tests"))
      .withoutDynamicAnalysis()
      .setProperty("sonar.profile.java", "empty")
      .addGoal(sonarGoal());
    orchestrator.executeBuilds(newBuild("test/with-tests"), analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:with-tests",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasure("tests")).isNull();
    assertThat(project.getMeasure("coverage")).isNull();
    assertThat(project.getMeasure("test_failures")).isNull();
    assertThat(project.getMeasure("test_errors")).isNull();
    assertThat(project.getMeasure("test_success_density")).isNull();
    assertThat(project.getMeasure("skipped_tests")).isNull();
    assertThat(project.getMeasure("test_execution_time")).isNull();
  }

  /**
   * See SONAR-401
   */
  @Test
  public void surefireShouldBeDisabled() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/disable-surefire"))
      .setProperty("sonar.profile.java", "empty")
      .addGoal(sonarGoal());
    orchestrator.executeBuilds(newBuild("test/disable-surefire"), analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:disable-surefire",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("coverage")).isEqualTo(0);
    assertThat(project.getMeasure("test_failures")).isNull();
    assertThat(project.getMeasure("test_errors")).isNull();
    assertThat(project.getMeasure("test_success_density")).isNull();
    assertThat(project.getMeasure("skipped_tests")).isNull();
    assertThat(project.getMeasure("test_execution_time")).isNull();
  }

  /**
   * See SONAR-3786
   */
  @Test
  public void shouldHaveTestFailures() {
    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/test-failures"))
      .setProperty("sonar.profile.java", "empty")
      .addGoal(sonarGoal());
    orchestrator.executeBuilds(newBuild("test/test-failures"), analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.test-failures:parent",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(6);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(33.3, Delta.delta(0.1));
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("test_success_density")).isEqualTo(50);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(1);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);

    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("test-failures",
        "/selenium/test-failures/SONAR-1099-html-characters.html",
        "/selenium/test-failures/tests-viewer.html", // SONAR-3786
        "/selenium/test-failures/tests-viewer-with-skipped-test.html" // SONAR-3786
      ).build();
      orchestrator.executeSelenese(selenese);
    }
  }

  @Test
  public void shouldReuseSurefireReports() {
    MavenBuild build = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/reuse-surefire-reports"))
      .setGoals("clean", "install"); // surefire are executed during build

    MavenBuild analysis = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/reuse-surefire-reports"))
      .addGoal(sonarGoal())
      .setProperty("sonar.profile.java", "empty")
      .setProperty("sonar.dynamicAnalysis", "reuseReports");
    orchestrator.executeBuilds(build, analysis);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:reuse-surefire-reports",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("coverage")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);
  }

  /**
   * SONAR-2841
   */
  @Test
  public void shouldNotReuseTestSuiteReportFileIfNotAlone() {
    MavenBuild build = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/reuse-surefire-reports"))
      .addGoal(sonarGoal())
      .setProperty("sonar.profile.java", "empty")
      .setProperty("sonar.dynamicAnalysis", "reuseReports")
      .setProperty("sonar.surefire.reportsPath", "existing-test-and-testsuite-reports");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:reuse-surefire-reports",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("coverage")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);
  }

  /**
   * SONAR-2841
   */
  @Test
  public void shouldReuseTestSuiteReportFileIfAlone() {
    MavenBuild build = MavenBuild.create()
      .setPom(ItUtils.locateProjectPom("test/reuse-surefire-reports"))
      .addGoal(sonarGoal())
      .setProperty("sonar.profile.java", "empty")
      .setProperty("sonar.dynamicAnalysis", "reuseReports")
      .setProperty("sonar.surefire.reportsPath", "existing-testsuite-report");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:reuse-surefire-reports",
      "test_success_density", "test_failures", "test_errors", "tests", "skipped_tests", "test_execution_time", "coverage"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("test_failures")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_errors")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("coverage")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("skipped_tests")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("test_execution_time")).isGreaterThan(0);
  }

}
