/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.history.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;


public class TimeMachinePageTest {

  @ClassRule
  public static Orchestrator orchestrator = HistoryTestSuite.ORCHESTRATOR;

  // SONAR-3006
  @Test
  public void test_time_machine_dashboard() {
    orchestrator.resetData();
    analyzeProject("2012-09-01", "0.7");
    analyzeProject("2012-10-15", "0.8");
    analyzeProject("2012-11-30", "0.9");
    analyzeProject("2012-12-31", "1.0-SNAPSHOT");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("timemachine",
      "/selenium/timemachine/should-display-timemachine-dashboard.html").build();
    orchestrator.executeSelenese(selenese);
  }

  private static void analyzeProject(String date, String version) {
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
        .setCleanPackageSonarGoals()
        .setProperties("sonar.projectDate", date)
        .setProperties("sonar.projectVersion", version)
    );
  }

}
