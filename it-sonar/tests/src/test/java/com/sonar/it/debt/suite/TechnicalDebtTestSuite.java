/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.debt.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TechnicalDebtMeasureTest.class, TechnicalDebtWidgetTest.class, TechnicalDebtPurgeTest.class, TechnicalDebtTest.class, NewTechnicalDebtMeasureTest.class,
  TechnicalDebtInIssueChangelogTest.class, SqaleRatingMeasureTest.class
})
public class TechnicalDebtTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();
}
