/*
 * Copyright (C) 2009-2014 SonarSource SA
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
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

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
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent package", "sonar:sonar")
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
    List<Issue> issues = findIssues(HELLO_CLASS, "common-java:DuplicatedBlocks");
    assertThat(issues.size()).isEqualTo(1);
  }

  /**
   * SONAR-3497
   */
  @Test
  public void shouldFindInsufficientCommentViolation() {
    List<Issue> issues = findIssues(HELLO_CLASS, "common-java:InsufficientCommentDensity");
    assertThat(issues.size()).isEqualTo(1);
  }

  /**
   * SONAR-3498
   */
  @Test
  public void shouldFindInsufficientCoverageViolation() {
    List<Issue> issues = findIssues(HELLO_CLASS, "common-java:InsufficientBranchCoverage");
    assertThat(issues.size()).isEqualTo(1);

    issues = findIssues(HELLO_CLASS, "common-java:InsufficientLineCoverage");
    assertThat(issues.size()).isEqualTo(1);
  }

  /**
   * SONARPLUGINS-2682
   */
  @Test
  public void shouldFindSkippedUnitTest() {
    List<Issue> issues = findIssues("com.sonarsource.it.samples:sonar-common-rules-project:src/test/java/IgnoredTest.java", "common-java:SkippedUnitTests");
    assertThat(issues.size()).isEqualTo(1);
  }

  /**
   * SONARPLUGINS-2682
   */
  @Test
  public void shouldFindFailedAndErrorUnitTests() {
    List<Issue> issues = findIssues("com.sonarsource.it.samples:sonar-common-rules-project:src/test/java/FailingTest.java", "common-java:FailedUnitTests");
    assertThat(issues.size()).isEqualTo(1);

    issues = findIssues("com.sonarsource.it.samples:sonar-common-rules-project:src/test/java/ErrorTest.java", "common-java:FailedUnitTests");
    assertThat(issues.size()).isEqualTo(1);
  }

  private List<Issue> findIssues(String componentKey, String ruleKey) {
    return orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().components(componentKey).rules(ruleKey)).list();
  }
}
