/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-4776
 */
public class NewTechnicalDebtMeasureTest {

  @ClassRule
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void new_technical_debt_measures_from_new_issues() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("sonar.projectDate", "2013-01-01"));

    // Second analysis -> issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));

    // New technical debt only comes from new issues
    Resource newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(13);
    assertThat(measures.get(0).getVariation2()).isEqualTo(13);

    // Third analysis, with exactly the same profile -> no new issues so no new technical debt
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));

    // No variation => measure is purged
    assertThat(newTechnicalDebt).isNull();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_previous_analysis() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over 30 days"

    // Execute twice analysis
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).setProfile("one-issue-per-file"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")).setProfile("one-issue-per-file"));

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    Resource newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(90);

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(0);
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_30_days() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("sonar.projectDate", "2013-01-01"));

    // Second analysis -> issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file"));

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    Resource newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation2()).isEqualTo(90);

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis but still since 30 days
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation2()).isEqualTo(90);
  }

  /**
   * SONAR-5059
   */
  @Test
  public void new_technical_debt_measures_should_never_be_negative() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis with a big effort to fix
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-file.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "100"));

    // Execute a second analysis with a smaller effort to fix -> Added technical debt should be 0, not negative
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-file")
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    Resource newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    assertThat(newTechnicalDebt).isNull();
  }

}
