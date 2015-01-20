/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.duplications.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  CrossProjectDuplicationsTest.class, DuplicationsTest.class
})
public class DuplicationsTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    // Used by DuplicationsTest
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-useless-code-tracker-plugin", "1.0"))
    .addPlugin("java")
    .build();
}
