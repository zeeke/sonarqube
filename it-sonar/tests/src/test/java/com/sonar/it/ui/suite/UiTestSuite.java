/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  DashboardTest.class,
  CrossProjectDuplicationsTest.class,
  UnitTestTest.class,
  IntegrationTestTest.class,
  HighlightingTest.class,
  EncodingTest.class
})
public class UiTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("ui-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin("java")
    .build();
}
