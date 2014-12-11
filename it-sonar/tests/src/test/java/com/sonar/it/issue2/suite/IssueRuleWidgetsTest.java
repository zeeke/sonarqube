/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.Build;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class IssueRuleWidgetsTest extends AbstractIssueTestCase2 {

  @Before
  public void before() throws Exception {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
    analyzeProject();
  }

  /**
   * SONAR-4341
   */
  @Test
  public void test_rules_widgets() throws Exception {
    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("rules-widget",
        "/selenium/issue/widgets/rules/should-have-correct-values.html",
        "/selenium/issue/widgets/rules/should-open-issues-by-severity.html",
        "/selenium/issue/widgets/rules/should-open-issues-count.html"
      ).build());
  }

  /**
   * SONAR-3081
   * SONAR-4341
   */
  @Test
  @Ignore
  public void test_rules_widgets_on_differential_view() throws Exception {
    // let's exclude 1 file to have cleared issues
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("rule/rule-widgets"))
      .setProperties("sonar.exclusions", "**/FewViolations.java", "sonar.profile", "sonar-way-2.7"));

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("rules-widget-differential-view-cleared-issues",
        "/selenium/issue/widgets/rules/diff-view-should-show-cleared-issues-count.html"
      ).build());

    // And let's run again to have new issues
    analyzeProject();

    orchestrator.executeSelenese(Selenese
      .builder()
      .setHtmlTestsInClasspath("rules-widget-differential-view-new-issues",
        "/selenium/issue/widgets/rules/diff-view-should-show-new-issues-count.html",
        "/selenium/issue/widgets/rules/diff-view-should-open-new-issues-on-drilldown.html"
      ).build());
  }

  private void analyzeProject() {
    Build scan = SonarRunner.create(ItUtils.locateProjectDir("issue/rule-widgets"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.profile", "sonar-way-2.7", "sonar.cpd.skip", "true");
    orchestrator.executeBuild(scan);
  }

}
