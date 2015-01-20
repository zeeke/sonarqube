/*
 * Copyright (C) 2009-2014 SonarSource SA
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
  CustomMetricTest.class, ExtensionLifecycleTest.class, FileDependencyTest.class, LinksTest.class, MavenTest.class, ProjectBuilderTest.class, ProjectExclusionsTest.class,
  SemaphoreTest.class, SqlLogsTest.class,
  IncrementalModeTest.class, TempFolderTest.class, MultiLanguageTest.class, IssueJsonReportTest.class, ProjectProvisioningTest.class, DependencyTest.class
})
public class BatchTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.javaPlugin())
    .setContext("/")

    // used by TempFolderTest
    .addPlugin(ItUtils.locateTestPlugin("batch-plugin"))

    // used by MavenTest
    .addPlugin(ItUtils.locateTestPlugin("maven-execution-plugin"))

    // used by CustomMetricTest
    .addPlugin(ItUtils.locateTestPlugin("custom-metric-plugin"))

    // used by ExtensionLifecycleTest
    .addPlugin(ItUtils.locateTestPlugin("extension-lifecycle-plugin"))

    // used by ProjectBuilderTest
    .addPlugin(ItUtils.locateTestPlugin("project-builder-plugin"))

    // used by SemaphoreTest
    .addPlugin(ItUtils.locateTestPlugin("crash-plugin"))

    .build();
}
