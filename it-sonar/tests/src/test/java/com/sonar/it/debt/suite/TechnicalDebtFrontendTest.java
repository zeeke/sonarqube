/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;

public class TechnicalDebtFrontendTest {

  @ClassRule
  public static Orchestrator orchestrator = TechnicalDebtTestSuite.ORCHESTRATOR;

  @Before
  public void resetData() {
    orchestrator.resetData();

    // Set hours in day property to default value (should be 8)
    orchestrator.getServer().getAdminWsClient().delete(
      new PropertyDeleteQuery("sonar.technicalDebt.hoursInDay"));
  }

  /**
   * SONAR-5413
   */
  @Test
  public void frontend_uses_config_for_hours_in_day() {
    orchestrator.executeSelenese(Selenese.builder()
      .setHtmlTestsInClasspath("use-default-value",
        "/selenium/debt/technical-debt-frontend/use-default-value.html"
      ).build());
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.technicalDebt.hoursInDay", "9"));
    orchestrator.executeSelenese(Selenese.builder()
      .setHtmlTestsInClasspath("use-config-value",
        "/selenium/debt/technical-debt-frontend/use-config-value.html"
      ).build());
  }
}
