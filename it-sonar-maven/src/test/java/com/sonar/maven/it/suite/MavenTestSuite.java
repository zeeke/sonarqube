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
  LinksTest.class, MavenTest.class, SourceFiltersTest.class, CoverageExclusionsTest.class,
  Struts139Test.class, IntegrationTestTest.class, JacocoTest.class, UnitTestTest.class, OldMultiLanguageTest.class, DependencyTest.class
})
public class MavenTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())

    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.javascript", "sonar-javascript-plugin", "2.0"))
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.python", "sonar-python-plugin", "1.3"))
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-web-plugin", "2.2"))
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.xml", "sonar-xml-plugin", "1.2"))

    .build();
}
