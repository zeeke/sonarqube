/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  BatchTest.class, FileExclusionsTest.class, LinksTest.class, MavenTest.class, ProjectExclusionsTest.class, SqlLogsTest.class, DryRunTest.class
})
public class BatchTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
      .addPlugin(ItUtils.xooPlugin())
      .setContext("/")

      // used by MavenTest
      .addPlugin(ItUtils.locateTestPlugin("maven-execution-plugin"))
      // used by DryRunTest
      .addPlugin(ItUtils.locateTestPlugin("access-secured-props-plugin"))
      .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-build-breaker-plugin", "1.1"))

      .build();
}
