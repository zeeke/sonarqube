/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.Test;

public class IntegrationTestTest extends AbstractMavenTest {

  @BeforeClass
  public static void inspectProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("test/jacoco-integration-tests")).setGoals(cleanInstallSonarGoal());
    orchestrator.executeBuilds(build);
  }

  @Test
  public void testIntegrationTestWidget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("it-widget",
      "/selenium/test/integration-test-widget.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
