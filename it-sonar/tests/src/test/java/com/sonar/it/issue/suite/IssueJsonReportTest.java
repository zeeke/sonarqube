/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.wsclient.services.PropertyCreateQuery;

import java.io.File;

import static com.sonar.it.ItUtils.sanitizeTimezones;
import static org.fest.assertions.Assertions.assertThat;

public class IssueJsonReportTest extends AbstractIssueTestCase {

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void test_json_report() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTrackingTest/issue-tracking-profile.xml"));

    File rootPomV1 = ItUtils.locateProjectPom("issue/tracking-v1");
    MavenBuild scan = MavenBuild.create(rootPomV1)
      .setCleanSonarGoals()
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProperty("sonar.profile.java", "issue-tracking");
    orchestrator.executeBuild(scan);

    // Dry-run scan -> 2 new issues and 2 existing issues
    File rootPomV2 = ItUtils.locateProjectPom("issue/tracking-v2");
    MavenBuild dryRunScan = MavenBuild.create(rootPomV2)
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      .setProperty("sonar.profile.java", "issue-tracking");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(rootPomV2.getParentFile(), "target/sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/issue/suite/IssueJsonReportTest/report-on-single-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  @Test
  public void test_json_report_on_branch() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTrackingTest/issue-tracking-profile.xml"));

    File rootPomV1 = ItUtils.locateProjectPom("issue/tracking-v1");
    MavenBuild scan = MavenBuild.create(rootPomV1)
      .setCleanSonarGoals()
      .setProperty("sonar.projectDate", "2013-05-01")
      .setProperty("sonar.branch", "mybranch")
      .setProfile("")
      .setProperty("sonar.profile.java", "issue-tracking");
    orchestrator.executeBuild(scan);

    // Dry-run scan -> 2 new issues and 2 existing issues
    File rootPomV2 = ItUtils.locateProjectPom("issue/tracking-v2");
    MavenBuild dryRunScan = MavenBuild.create(rootPomV2)
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      .setProperty("sonar.branch", "mybranch")
      .setProperty("sonar.profile.java", "issue-tracking");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(rootPomV2.getParentFile(), "target/sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/issue/suite/IssueJsonReportTest/report-on-single-module-branch.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project but Eclipse scans only a single module
   */
  @Test
  public void test_json_report_on_sub_module() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));

    File rootPom = ItUtils.locateProjectPom("shared/multi-modules-sample");
    MavenBuild scan = MavenBuild.create(rootPom)
      .setCleanSonarGoals()
      .setProperty("sonar.projectDate", "2013-05-01")
      // Should force locale to have checkstyle/PMD violation messages in english
      .setEnvironmentVariable("MAVEN_OPTS", "-Duser.language=en -Duser.region=US")
      .setProperty("sonar.profile.java", "issues");
    orchestrator.executeBuild(scan);

    // SONAR-4342 Set property in DB on root module
    orchestrator.getServer().getAdminWsClient().create(new PropertyCreateQuery("sonar.profile.java", "issues", "com.sonarsource.it.samples:multi-modules-sample"));

    // Dry-run scan on a module -> no new issues
    File modulePom = ItUtils.locateProjectPom("shared/multi-modules-sample/module_a/module_a1");
    MavenBuild dryRunScan = MavenBuild.create(modulePom)
      .setCleanSonarGoals()
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      // Should force locale to have checkstyle/PMD violation messages in english
      .setEnvironmentVariable("MAVEN_OPTS", "-Duser.language=en -Duser.region=US")
      .setProfile(""); // Use profile defined in DB
    orchestrator.executeBuild(dryRunScan);

    File report = new File(modulePom.getParentFile(), "target/sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/issue/suite/IssueJsonReportTest/report-on-sub-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  /**
   * Multi-modules project
   */
  @Test
  public void test_json_report_on_root_module() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));

    File rootPom = ItUtils.locateProjectPom("shared/multi-modules-sample");
    MavenBuild scan = MavenBuild.create(rootPom)
      .setCleanSonarGoals()
      .setProperty("sonar.projectDate", "2013-05-01")
      // Checkstyle issues messages are localized
      .setEnvironmentVariable("MAVEN_OPTS", "-Duser.language=en -Duser.region=US")
      .setProperty("sonar.profile.java", "issues");
    orchestrator.executeBuild(scan);

    // Dry-run scan -> no new issues
    MavenBuild dryRunScan = MavenBuild.create(rootPom)
      .setCleanSonarGoals()
      .setProperty("sonar.dryRun", "true")
      .setProperty("sonar.projectDate", "2013-05-02")
      // Checkstyle issues messages are localized
      .setEnvironmentVariable("MAVEN_OPTS", "-Duser.language=en -Duser.region=US")
      .setProperty("sonar.profile.java", "issues");
    orchestrator.executeBuild(dryRunScan);

    File report = new File(rootPom.getParentFile(), "target/sonar/sonar-report.json");
    assertThat(report).isFile().exists();

    String json = sanitize(FileUtils.readFileToString(report));
    String expectedJson = sanitize(IOUtils.toString(getClass().getResource("/com/sonar/it/issue/suite/IssueJsonReportTest/report-on-root-module.json")));
    JSONAssert.assertEquals(expectedJson, json, false);
  }

  private static String sanitize(String s) {
    // sanitize issue uuid keys
    s = s.replaceAll("\\w\\w\\w\\w\\w\\w\\w\\w\\-\\w\\w\\w\\w\\-\\w\\w\\w\\w\\-\\w\\w\\w\\w\\-\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w\\w", "abcde");

    // sanitize sonar version. Note that "-SILVER-SNAPSHOT" is used by Goldeneye jobs
    s = s.replaceAll("\\d\\.\\d(.\\d)?(\\-SILVER)?\\-SNAPSHOT", "<SONAR_VERSION>");

    return sanitizeTimezones(s);
  }

}
