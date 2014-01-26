/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.java.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class ChidamberKemererTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("java/chidamber-kemerer"))
      .setGoals("clean verify", "sonar:sonar")
      .setProperty("sonar.language", "java")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testCkjmWidgets() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("java-ckjm-widgets",
      "/selenium/java/rfc-widget.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
