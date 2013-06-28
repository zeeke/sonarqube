/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;

public class UpdateCenterWithCompatiblePluginOnSonarUpgradeTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.updatecenter.url", UpdateCenterWithCompatiblePluginOnSonarUpgradeTest.class.getResource(
      "/com/sonar/it/server/UpdateCenterTest/update-center-system-update-with-already-compatible-plugins.properties").toString())
    .addPlugin(ItUtils.locateTestPlugin("sonar-fake-plugin"))
    .build();

  /**
   * SONAR-4279
   */
  @Test
  public void should_not_display_already_compatible_plugins_on_system_update() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("system-updates-without-plugin-updates",
      "/selenium/server/updatecenter/system-updates-without-plugin-updates.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

}
