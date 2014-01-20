/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.ViolationQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SonarCommonRulesTest {

  private static final String HELLO_CLASS = "com.sonarsource.it.samples:sonar-common-rules-project:src/main/java/Hello.java";

  @ClassRule
  public static Orchestrator orchestrator = RuleTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/rule/SonarCommonRulesTest/sonar_common_rules_profile.xml"));
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("rule/sonar-common-rules-project"))
      .setCleanPackageSonarGoals()
      .setProfile("sonar_common_rules_profile")
      .setProperty("maven.test.error.ignore", "true")
      .setProperty("maven.test.failure.ignore", "true");
    orchestrator.executeBuild(build);
  }

  /**
   * SONAR-3496
   */
  @Test
  public void shouldFindDuplicationViolation() {
    List<Violation> violations = findViolations(HELLO_CLASS, "common-java:DuplicatedBlocks");
    assertThat(violations.size()).isEqualTo(1);
  }

  /**
   * SONAR-3497
   */
  @Test
  public void shouldFindInsufficientCommentViolation() {
    List<Violation> violations = findViolations(HELLO_CLASS, "common-java:InsufficientCommentDensity");
    assertThat(violations.size()).isEqualTo(1);
  }

  /**
   * SONAR-3498
   */
  @Test
  public void shouldFindInsufficientCoverageViolation() {
    List<Violation> violations = findViolations(HELLO_CLASS, "common-java:InsufficientBranchCoverage");
    assertThat(violations.size()).isEqualTo(1);

    violations = findViolations(HELLO_CLASS, "common-java:InsufficientLineCoverage");
    assertThat(violations.size()).isEqualTo(1);
  }

  /**
   * SONARPLUGINS-2682
   */
  @Test
  public void shouldFindSkippedUnitTest() {
    List<Violation> violations = findViolations("com.sonarsource.it.samples:sonar-common-rules-project:src/test/java/IgnoredTest.java", "common-java:SkippedUnitTests");
    assertThat(violations.size()).isEqualTo(1);
  }

  /**
   * SONARPLUGINS-2682
   */
  @Test
  public void shouldFindFailedAndErrorUnitTests() {
    List<Violation> violations = findViolations("com.sonarsource.it.samples:sonar-common-rules-project:src/test/java/FailingTest.java", "common-java:FailedUnitTests");
    assertThat(violations.size()).isEqualTo(1);

    violations = findViolations("com.sonarsource.it.samples:sonar-common-rules-project:src/test/java/ErrorTest.java", "common-java:FailedUnitTests");
    assertThat(violations.size()).isEqualTo(1);
  }

  private List<Violation> findViolations(String resourceKey, String violationId) {
    List<Violation> violations = orchestrator.getServer().getWsClient().findAll(ViolationQuery.createForResource(resourceKey).setRuleKeys(violationId));
    return violations;
  }
}
