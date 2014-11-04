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

public class EncodingTest {

  @ClassRule
  public static Orchestrator orchestrator = UiTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void shouldSupportJapaneseCharset() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("java/japanese-charset"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("java-japanese-charset",
      "/selenium/java-japanese-charset/japanese_sources.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
