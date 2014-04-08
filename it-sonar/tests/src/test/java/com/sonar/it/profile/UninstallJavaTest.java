/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Test;

public class UninstallJavaTest {

  /**
   * SONAR-4139
   */
  @Test
  public void should_not_display_java_quality_profile_when_java_plugin_is_removed() {
    Orchestrator orchestrator = Orchestrator.builderEnv()
        // Add javascript plugin because orchestrator 2.6 has an issue when there's no language plugin
        .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.javascript", "sonar-javascript-plugin", "1.1"))
        .build();
    orchestrator.start();

    // Remove java plugin with updatecenter web console because there's no way to do that with the Orchestrator API.
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("uninstall_sonar_java",
      "/selenium/profile/uninstall-java/uninstall_sonar_java.html"
    ).build());

    orchestrator.restartSonar();

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("check_quality_profiles",
      "/selenium/profile/uninstall-java/check_only_javascript_quality_profile_exists.html",
      "/selenium/profile/uninstall-java/create_a_javascript_profile.html"
    ).build());

    orchestrator.stop();
  }
}
