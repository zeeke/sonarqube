/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class RuleWidgetsTest {

  @ClassRule
  public static Orchestrator orchestrator = RuleTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setup() throws Exception {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("rule/rule-widgets"))
      .setProperties("sonar.profile", "sonar-way-2.7"));
  }

  // SONAR-2070
  @Test
  public void test_most_violated_rules_widgets() throws Exception {
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("most-violated-rules-widget",
        "/selenium/rule/widgets/most-violated-rules/most-violated-rules-widget.html",
        "/selenium/rule/widgets/most-violated-rules/most-violated-rules-popup-on-violations.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }
}
