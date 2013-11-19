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
import org.fest.assertions.Delta;
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
  public static Orchestrator orchestrator = DebtTestSuite.ORCHESTRATOR;

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
    Resource newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1().doubleValue()).isEqualTo(0.027, Delta.delta(0.001));
    assertThat(measures.get(0).getVariation2().doubleValue()).isEqualTo(0.027, Delta.delta(0.001));

    // Third analysis, with exactly the same profile -> no new issues so no new technical debt
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));

    // No variation => measure is purged
    assertThat(newTechnicalDebt).isNull();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("sonar.projectDate", "2013-01-01"));

    // Second analysis -> issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/one-issue-per-line.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line"));

    // Third analysis, existing issues on OneIssuePerLine will have their technical debt updated with the effort to fix
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line")
      .setProperties("sonar.oneIssuePerLine.effortToFix", "10"));

    Resource newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1().doubleValue()).isEqualTo(0.243, Delta.delta(0.001));
    assertThat(measures.get(0).getVariation2().doubleValue()).isEqualTo(0.243, Delta.delta(0.001));

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis but still since 30 days
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line")
      .setProperties("sonar.oneIssuePerLine.effortToFix", "10"));

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1().doubleValue()).isEqualTo(0d);
    assertThat(measures.get(0).getVariation2().doubleValue()).isEqualTo(0.243, Delta.delta(0.001));
  }

}
