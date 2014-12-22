/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.*;

import java.sql.Connection;
import java.sql.SQLException;

public class ManualRulesTest {

  @ClassRule
  public static Orchestrator orchestrator = RuleTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setup() throws Exception {
    orchestrator.resetData();
    deleteManualRules();
  }

  @AfterClass
  public static void purgeManualRules() {
    deleteManualRules();
  }

  @Test
  @Ignore
  // TODO Enable for new coding rules page
  public void testManualRules() {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("manual-rules",
        "/selenium/rule/manual-rules/create_edit_delete_manual_rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  protected static void deleteManualRules(){
    try {
      Connection connection = orchestrator.getDatabase().openConnection();
      connection.prepareStatement("DELETE FROM rules WHERE rules.plugin_name='manual'").execute();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to remove manual rules", e);
    }
  }

}
