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

import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;

public class PurgeTest {

  @ClassRule
  public static Orchestrator orchestrator = DebtTestSuite.ORCHESTRATOR;

  private static final String SQL_COUNT_MEASURES_ON_CHARACTERISTICS = "select count(*) from project_measures where characteristic_id in (select id from characteristics where rule_id is null)";
  private static final String SQL_COUNT_MEASURES_ON_REQUIREMENTS = "select count(*) from project_measures where characteristic_id in (select id from characteristics where rule_id is not null)";

  @Before
  public void resetData() throws Exception {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-2756
   */
  @Test
  public void purge_measures_on_requirements() throws SQLException {
    scanProject("2012-01-01");
    int onCharacteristicsCount = orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_CHARACTERISTICS);
    int onRequirementsCount = orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_REQUIREMENTS);
    assertThat(onCharacteristicsCount).isGreaterThan(0);
    assertThat(onRequirementsCount).isGreaterThan(0);

    scanProject("2012-02-02");
    // past measures on characteristics are not purged
    assertThat(orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_CHARACTERISTICS)).isGreaterThan(onCharacteristicsCount);

    // past measures on characteristics are purged
    assertThat(orchestrator.getDatabase().countSql(SQL_COUNT_MEASURES_ON_REQUIREMENTS)).isEqualTo(onRequirementsCount);
  }

  private static void scanProject(String date) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/debt/with-many-rules.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.projectDate", date)
      .withoutDynamicAnalysis()
      .setProfile("with-many-rules");
    orchestrator.executeBuild(scan);
  }
}
