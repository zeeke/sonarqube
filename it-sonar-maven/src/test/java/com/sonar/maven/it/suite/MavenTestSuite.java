/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  LinksTest.class, MavenTest.class, DesignUITest.class, DuplicationsTest.class, SourceFiltersTest.class, CoverageExclusionsTest.class,
  JavaTest.class, Struts139Test.class, IntegrationTestTest.class, JacocoTest.class, UnitTestTest.class, OldMultiLanguageTest.class, EventTest.class
})
public class MavenTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())

    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.javascript", "sonar-javascript-plugin", "1.5"))
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.python", "sonar-python-plugin", "1.0"))

    .build();
}
