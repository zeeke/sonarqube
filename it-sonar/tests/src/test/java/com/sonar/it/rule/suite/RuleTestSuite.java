/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ManualRulesTest.class, RuleWidgetsTest.class, SonarCommonRulesTest.class, RulesTest.class
})
public class RuleTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("deprecated-xoo-rule-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin", "2.1.1"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1"))
    .addPlugin("java")
    .build();
}
