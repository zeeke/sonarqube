/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;

public class ManualRulesTest {

  @ClassRule
  public static Orchestrator orchestrator = RuleTestSuite.ORCHESTRATOR;

  @Test
  public void testManualRules() {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("manual-rules",
        "/selenium/rule/manual-rules/create_edit_delete_manual_rule.html",

        // SONAR-3359
        "/selenium/rule/manual-rules/create_manual_rule_without_description.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

}
