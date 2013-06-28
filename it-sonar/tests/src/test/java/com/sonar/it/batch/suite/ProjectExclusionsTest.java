/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProjectExclusionsTest {
  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Before
  public void deleteProjectData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * This use-case was a bug in 2.8-RC2. It failed when both the properties sonar.branch and sonar.skippedModules
   * were set on the same multi-modules project.
   */
  @Test
  public void shouldSupportMixOfBranchAndSkippedModules() {
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .addGoal("clean verify")
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.branch", "mybranch")
      .withProperty("sonar.skippedModules", "module_b")
      .build();

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample:mybranch"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a:mybranch").getId());
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1:mybranch").getId());
    assertNotNull(getResource("com.sonarsource.it.samples:module_a2:mybranch").getId());

    assertNull(getResource("com.sonarsource.it.samples:module_b:mybranch"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1:mybranch"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2:mybranch"));
  }

  /**
   * Black list
   */
  @Test
  public void shouldExcludeModuleAndItsChildren() {
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .addGoal("clean verify")
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.skippedModules", "module_b")
      .build();

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a2"));

    // excluded project and its children
    assertNull(getResource("com.sonarsource.it.samples:module_b"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2"));
  }

  /**
   * Exhaustive white list
   */
  @Test
  public void shouldIncludeModules() {
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .addGoal("clean verify")
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.includedModules", "multi-modules-sample,module_a,module_a1")
      .build();

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1"));

    assertNull(getResource("com.sonarsource.it.samples:module_a2"));
    assertNull(getResource("com.sonarsource.it.samples:module_b"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2"));
  }

  @Test
  public void rootModuleShouldBeOptionalInListOfIncludedModules() {
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.includedModules", "module_a,module_a1") // the root module 'multi-modules-sample' is not declared
      .build();

    orchestrator.executeBuild(build);

    assertNotNull(getResource("com.sonarsource.it.samples:multi-modules-sample"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a"));
    assertNotNull(getResource("com.sonarsource.it.samples:module_a1"));

    assertNull(getResource("com.sonarsource.it.samples:module_a2"));
    assertNull(getResource("com.sonarsource.it.samples:module_b"));
    assertNull(getResource("com.sonarsource.it.samples:module_b1"));
    assertNull(getResource("com.sonarsource.it.samples:module_b2"));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.create(key));
  }
}
