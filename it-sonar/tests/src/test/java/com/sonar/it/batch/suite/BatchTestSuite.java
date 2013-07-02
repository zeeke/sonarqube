/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  BatchTest.class, FileExclusionsTest.class, LinksTest.class, MavenTest.class, ProjectExclusionsTest.class, SqlLogsTest.class
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

    .build();
}
