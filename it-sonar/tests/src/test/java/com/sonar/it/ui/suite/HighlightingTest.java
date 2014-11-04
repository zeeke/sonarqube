/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class HighlightingTest {

  @ClassRule
  public static Orchestrator orchestrator = UiTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  // SONAR-3893 & SONAR-4247
  @Test
  public void shouldHighlightJavaSourceCode() throws Exception {

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample-with-tests"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("java-syntax-highlighting",
      "/selenium/ui/highlighting/syntax-highlighting.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4249 & SONAR-4250
  @Test
  public void shouldHighlightJavaSymbolsUsage() throws Exception {

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample-with-tests"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("highlight-symbol-usages",
      "/selenium/ui/highlighting/symbol-usages-highlighting.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
