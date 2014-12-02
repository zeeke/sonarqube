/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;

import static com.sonar.it.ItUtils.sanitizeTimezones;
import static org.fest.assertions.Assertions.assertThat;

public class IssueJsonReportTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void test_json_report() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("batch/tracking/v1"))
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    // Dry-run scan -> 2 new issues and 13 existing issues
    File projectDir = ItUtils.locateProjectDir("batch/tracking/v2");
    SonarRunner dryRunScan = SonarRunner.create(projectDir)
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/batch/IssueJsonReportTest/report-on-single-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void test_json_report_on_branch() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("batch/tracking/v1"))
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProperty("sonar.branch", "mybranch")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    // Dry-run scan -> 2 new issues and 13 existing issues
    File projectDir = ItUtils.locateProjectDir("batch/tracking/v2");
    SonarRunner dryRunScan = SonarRunner.create(projectDir)
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      .setProperty("sonar.branch", "mybranch")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(projectDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/batch/IssueJsonReportTest/report-on-single-module-branch.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project but Eclipse scans only a single module
   */
  @Test
  public void test_json_report_on_sub_module() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));

    orchestrator.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "Multi-module sample");
    orchestrator.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-line");

    File rootDir = ItUtils.locateProjectDir("shared/xoo-multi-modules-sample");
    SonarRunner scan = SonarRunner.create(rootDir)
      .setProperty("sonar.projectDate", "2013-05-01");
    orchestrator.executeBuild(scan);

    // Dry-run scan on a module -> no new issues
    File moduleDir = ItUtils.locateProjectDir("shared/xoo-multi-modules-sample/module_a/module_a1");
    SonarRunner dryRunScan = SonarRunner.create(moduleDir)
      .setProperty("sonar.projectKey", "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1")
      .setProperty("sonar.projectVersion", "1.0-SNAPSHOT")
      .setProperty("sonar.projectName", "ModuleA1")
      .setProperty("sonar.sources", "src/main/xoo")
      .setProperty("sonar.language", "xoo")
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(moduleDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    // SONAR-5218 All issues are updated as their root project id has changed (it's now the sub module)
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/batch/IssueJsonReportTest/report-on-sub-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project
   */
  @Test
  public void test_json_report_on_root_module() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));

    File rootDir = ItUtils.locateProjectDir("shared/xoo-multi-modules-sample");
    SonarRunner scan = SonarRunner.create(rootDir)
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    // Dry-run scan -> no new issues
    SonarRunner dryRunScan = SonarRunner.create(rootDir)
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(rootDir, ".sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/batch/IssueJsonReportTest/report-on-root-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void sanityCheck() {
    assertThat(sanitize("5.0.0-5868-SILVER-SNAPSHOT")).isEqualTo("<SONAR_VERSION>");
  }

  private static String sanitize(String s) {
    // sanitize issue uuid keys
    s = s.replaceAll("\\w\\w\\w\\w\\w\\w\\w\\w\\-\\w\\w\\w\\w\\-\\w\\w\\w\\w\\-\\w\\w\\w\\w\\-\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w", "abcde");

    // sanitize sonar version. Note that "-SILVER-SNAPSHOT" is used by Goldeneye jobs
    s = s.replaceAll("\\d\\.\\d(.\\d)?(\\-.*)?\\-SNAPSHOT", "<SONAR_VERSION>");

    return sanitizeTimezones(s);
  }

}
