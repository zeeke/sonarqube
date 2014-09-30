/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-4564
 */
public class NewIssuesMeasureTest {

  @ClassRule
  public static Orchestrator orchestrator = Issue2TestSuite.ORCHESTRATOR;

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @Test
  public void new_issues_measures() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("sonar.projectDate", "2013-01-01"));

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);

    assertThat(orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).list()).isNotEmpty();
    Resource newIssues = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_violations").setIncludeTrends(true));
    List<Measure> measures = newIssues.getMeasures();
    assertThat(measures.get(0).getVariation1().intValue()).isEqualTo(13);
    assertThat(measures.get(0).getVariation2().intValue()).isEqualTo(13);

    // second analysis, with exactly the same profile -> no new issues
    orchestrator.executeBuild(scan);

    assertThat(orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).list()).isNotEmpty();
    newIssues = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_violations").setIncludeTrends(true));
    // No variation => measure is purged
    assertThat(newIssues).isNull();
  }

  @Test
  public void new_issues_measures_should_be_zero_on_project_when_no_new_issues_since_x_days() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over 30 days"

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line")
      // Analyse a project in the past, with a date older than 30 last days (second period)
      .setProperty("sonar.projectDate", "2013-01-01"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));

    // new issues measures should be to 0 on project on 2 periods as new issues has been created
    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "new_violations").setIncludeTrends(true));
    List<Measure> measures = file.getMeasures();
    Measure newIssues = find(measures, "new_violations");
    assertThat(newIssues.getVariation1().intValue()).isEqualTo(0);
    assertThat(newIssues.getVariation2().intValue()).isEqualTo(0);
  }

  /**
   * SONAR-3647
   */
  @Test
  public void new_issues_measures_consistent_with_variations() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/NewIssuesMeasureTest/issue-on-tag-foobar.xml"));

    // Execute an analysis in the past to have a past snapshot
    // version 1
    SonarRunner firstScan = SonarRunner.create(ItUtils.locateProjectDir("history/xoo-tracking-v1"))
      .setProfile("issue-on-tag-foobar");
    orchestrator.executeBuilds(firstScan);

    // version 2 with 2 new violations and 3 more ncloc
    SonarRunner secondScan = SonarRunner.create(ItUtils.locateProjectDir("history/xoo-tracking-v2"))
      .setProfile("issue-on-tag-foobar");
    orchestrator.executeBuilds(secondScan);

    assertThat(orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).list()).isNotEmpty();
    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "new_violations", "violations", "ncloc").setIncludeTrends(true));
    List<Measure> measures = file.getMeasures();
    Measure newIssues = find(measures, "new_violations");
    assertThat(newIssues.getVariation1().intValue()).isEqualTo(2);
    assertThat(newIssues.getVariation2().intValue()).isEqualTo(2);

    Measure violations = find(measures, "violations");
    assertThat(violations.getValue().intValue()).isEqualTo(3);
    assertThat(violations.getVariation1().intValue()).isEqualTo(2);
    assertThat(violations.getVariation2().intValue()).isEqualTo(2);

    Measure ncloc = find(measures, "ncloc");
    assertThat(ncloc.getValue().intValue()).isEqualTo(16);
    assertThat(ncloc.getVariation1().intValue()).isEqualTo(3);
    assertThat(ncloc.getVariation2().intValue()).isEqualTo(3);
  }

  @Test
  public void new_issues_measures_should_be_correctly_calculated_when_adding_a_new_module() throws Exception {
    // This test assumes that period 1 is "since previous analysis"

    // First analysis without module b
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/NewIssuesMeasureTest/profile1.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProfile("profile1")
      .setProperties("sonar.skippedModules", "multi-modules-sample:module_b"));

    // Second analysis with module b and with a new rule activated to have new issues on module a since last analysis
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/NewIssuesMeasureTest/profile2.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProfile("profile2"));

    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:multi-modules-sample", "new_violations", "violations").setIncludeTrends(true));
    List<Measure> measures = project.getMeasures();
    Measure newIssues = find(measures, "new_violations");
    assertThat(newIssues.getVariation1().intValue()).isEqualTo(56);
  }

  /**
   * SONAR-4882
   */
  @Test
  public void new_severity_issues_by_rules_measures() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute multiples build by excluding different modules each times to have measures variation
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-many-rules.xml"));
    orchestrator.executeBuilds(
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProfile("with-many-rules").setProperty("sonar.skippedModules", "multi-modules-sample:module_b,multi-modules-sample:module_a:module_a2"),
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProfile("with-many-rules").setProperty("sonar.skippedModules", "multi-modules-sample:module_b"),
      SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
        .setProfile("with-many-rules")
      );

    String projectKey = "com.sonarsource.it.samples:multi-modules-sample";

    Resource newIssuesPerSeverities = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey,
      "new_blocker_violations", "new_critical_violations", "new_major_violations", "new_minor_violations", "new_info_violations"
      ).setIncludeTrends(true).setExcludeRules(false));

    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_blocker_violations")).isNull();

    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_critical_violations").getVariation1().intValue()).isEqualTo(2);
    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_critical_violations").getVariation2().intValue()).isEqualTo(3);

    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_major_violations").getVariation1().intValue()).isEqualTo(2);
    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_major_violations").getVariation2().intValue()).isEqualTo(3);

    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_minor_violations").getVariation1().intValue()).isEqualTo(24);
    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_minor_violations").getVariation2().intValue()).isEqualTo(36);

    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_info_violations").getVariation1().intValue()).isEqualTo(0);
    assertThat(find(newIssuesPerSeverities.getMeasures(), "new_info_violations").getVariation2().intValue()).isEqualTo(1);
  }

  private Measure find(List<Measure> measures, String metricKey) {
    for (Measure measure : measures) {
      if (measure.getMetricKey().equals(metricKey)) {
        return measure;
      }
    }
    return null;
  }

}
