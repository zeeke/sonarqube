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
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.ViolationQuery;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ViolationsTest {

  @ClassRule
  public static Orchestrator orchestrator = RuleTestSuite.ORCHESTRATOR;

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void testNoViolations() {
    // no active rules
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/sample"))
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.profile", "empty")
      .build();
    orchestrator.executeBuild(build);

    ViolationQuery query = ViolationQuery.createForResource("com.sonarsource.it.samples:simple-sample").setDepth(-1);
    assertThat(orchestrator.getServer().getWsClient().findAll(query).size(), is(0));

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:simple-sample", "violations", "blocker_violations", "violations_density"));
    assertThat(project.getMeasureIntValue("violations"), is(0));
    assertThat(project.getMeasureIntValue("blocker_violations"), is(0));
    assertThat(project.getMeasureIntValue("violations_density"), is(100));
  }

  /**
   * See SONAR-684
   */
  @Test
  public void shouldEncodeViolationMessages() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("rule/encoded-violation-message"))
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.profile", "sonar-way-2.7")
      .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("rule-encoded-violation-message",
      "/selenium/rule/violations/encoded-violation-message.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See SONAR-413
   */
  @Test
  public void shouldNotHaveViolationsOnTests() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/rule/ViolationsTest/backup.xml"));
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("rule/no-violations-on-tests"))
      .addGoal("clean verify")
      .withProperty("skipTests", "true")
      .withProperty("sonar.profile", "violations")
      .addSonarGoal()
      .withDynamicAnalysis(true)
      .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("rule-no-violations-on-tests",
      "/selenium/rule/violations/no-violations-on-tests.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }


  private List<Violation> getViolations(String resourceIdOrKey) {
    return orchestrator.getServer().getWsClient().findAll(ViolationQuery.createForResource(resourceIdOrKey));
  }

  private List<Violation> getBlockerViolations(String resourceIdOrKey) {
    return orchestrator.getServer().getWsClient().findAll(ViolationQuery.createForResource(resourceIdOrKey).setSeverities("BLOCKER"));
  }
}
