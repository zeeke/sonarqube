/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

@Ignore
public class ProfileChangelogWithStartupRemovedRulesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.locateTestPlugin("deprecated-xoo-rule-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("some-xoo-rules-plugin-v1", "some-xoo-rules-plugin", "1.0"))
    .build();

  /**
   * SONAR-4206
   * SONAR-4642
   */
  @Test
  public void add_removed_rules_to_changelog_when_rules_are_removed_at_startup() throws IOException {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath(
      "/com/sonar/it/profile/ProfileChangelogWithStartupRemovedRulesTest/profile_with_rule_to_be_removed_at_startup.xml"));

    // Execute a first analysis in order to track profile's changelog (see SONAR-2986)
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("profile_with_rule_to_be_removed_at_startup");
    orchestrator.executeBuild(runner);

    // Replace plugin v1.0 by plugin v2.0
    File pluginDir =  new File(orchestrator.getServer().getHome(), "/extensions/plugins");
    new File(new File(orchestrator.getServer().getHome(), "/extensions/plugins"), "some-xoo-rules-plugin-1.0.jar").delete();
    FileUtils.copyFileToDirectory(ItUtils.locateTestPlugin("some-xoo-rules-plugin-v2", "some-xoo-rules-plugin", "2.0").getFile(), pluginDir);
    orchestrator.restartSonar();

    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("add-removed-rules-in-changelog", "/selenium/profile-changelog/verify-changelog-with-startup-removed-rule.html").build()
    );
  }

  /**
   * SONAR-4642
   */
  @Test
  public void not_add_removed_rules_to_changelog_when_rules_on_no_more_existing_repo_are_removed_at_startup() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath(
      "/com/sonar/it/profile/ProfileChangelogWithStartupRemovedRulesTest/profile_with_rule_not_to_be_removed_at_startup.xml"));

    // Execute a first analysis in order to track profile's changelog (see SONAR-2986)
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("profile_with_rule_not_to_be_removed_at_startup");
    orchestrator.executeBuild(runner);

    // Remove plugin
    new File(new File(orchestrator.getServer().getHome(), "/extensions/plugins"), "deprecated-xoo-rule-plugin-1.0-SNAPSHOT.jar").delete();
    orchestrator.restartSonar();

    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("add-removed-rules-in-changelog", "/selenium/profile-changelog/verify-changelog-with-startup-not-removed-rule.html").build()
    );
  }

}
