/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  BatchTest.class, LinksTest.class, MavenTest.class, ProjectExclusionsTest.class, SqlLogsTest.class, PreviewModeTest.class,
  IncrementalModeTest.class, TempFolderTest.class, MultiLanguageTest.class, IssueJsonReportTest.class, ProjectProvisioningTest.class, DependencyTest.class
})
public class BatchTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .setContext("/")

    // used by BatchTest
    .addPlugin(ItUtils.locateTestPlugin("batch-plugin"))

    // used by MavenTest
    .addPlugin(ItUtils.locateTestPlugin("maven-execution-plugin"))

    // used by PreviewModeTest
    .addPlugin(ItUtils.locateTestPlugin("access-secured-props-plugin"))
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-build-breaker-plugin", "1.1"))

    // used by MultiLanguageTest
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.php", "sonar-php-plugin", "2.1"))

    .build();
}
