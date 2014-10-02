/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.test.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class IntegrationTestTest {
  @ClassRule
  public static Orchestrator orchestrator = TestTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void inspectProject() {
    orchestrator.resetData();
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("test/jacoco-integration-tests")).setGoals("clean", "install", "sonar:sonar");
    orchestrator.executeBuilds(build);
  }

  @Test
  public void testIntegrationTestWidget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("it-widget",
        "/selenium/test/integration-test-widget.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void testIntegrationTestViewer() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("integration-test-viewer",
        "/selenium/test/header-of-integration-tests-viewer.html",
        "/selenium/test/filter-integration-test-lines.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
