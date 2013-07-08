/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class ProfileChangelogWithStartupRemovedRulesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("deprecated-rule-plugin"))
      .build();

  @BeforeClass
  public static void restoreBackup() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/profile_with_rule_to_be_removed_at_startup.xml"));
  }

  /**
   * SONAR-4206
   */
  @Test
  public void should_add_removed_rules_to_changelog_when_rules_are_removed_at_startup() {
    // Remove rule plugin with updatecenter web console because there's no way to do that with the Orchestrator API.
    orchestrator.executeSelenese(
        Selenese.builder().setHtmlTestsInClasspath("remove-rule-plugin", "/selenium/profile-changelog/remove-rule-plugin.html").build()
    );
    orchestrator.restartSonar();

    orchestrator.executeSelenese(
        Selenese.builder().setHtmlTestsInClasspath("add-removed-rules-in-changelog", "/selenium/profile-changelog/verify-changelog-for-startup-removed-rule.html").build()
    );
  }

}
