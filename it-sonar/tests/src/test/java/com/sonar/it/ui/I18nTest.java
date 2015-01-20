/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class I18nTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("l10n-fr-pack"))
    .addPlugin(ItUtils.locateTestPlugin("self-l10ned-plugin"))
    .addPlugin("java")
    .build();

  @After
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * TODO This test should use a fake widget that display a fake metric with decimals instead of using provided metric
   */
  @Test
  public void test_localization() {

    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/sample"))
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-i18n",
      "/selenium/ui/i18n/default-locale-is-english.html",
      "/selenium/ui/i18n/french-locale.html",
      "/selenium/ui/i18n/french-pack.html",
      "/selenium/ui/i18n/locale-with-france-country.html",
      "/selenium/ui/i18n/locale-with-swiss-country.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void plugins_could_provide_their_own_bundles() {
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/sample"))
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .build();
    String logs = orchestrator.executeBuild(build).getLogs();

    assertThat(logs).contains("> Ceci est un message");
  }

}
