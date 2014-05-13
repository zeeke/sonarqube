/*
 * Copyright (C) 2009-2014 SonarSource SA
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
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class RemovingDeprecatedRulesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.locateTestPlugin("deprecated-xoo-rule-plugin"))
    .build();

  @BeforeClass
  public static void restoreBackup() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/RemovingDeprecatedRulesTest/profile_with_rule_to_be_removed_at_startup.xml"));
  }

  @Test
  public void remove_no_more_existing_rules_at_startup() {
    // Check deprecated rule exists in profile
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("verify-deprecated-rule-exists", "/selenium/profile/remove-deprecated-rules/verify-deprecated-rule-exists.html").build()
    );

    // Remove rule plugin with updatecenter web console
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("remove-deprecated-xoo-rule-plugin", "/selenium/profile/remove-deprecated-rules/remove-deprecated-xoo-rule-plugin.html").build()
    );
    orchestrator.restartSonar();

    // Check deprecated rule do not exists in profile
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("verify-deprecated-rule-is-removed", "/selenium/profile/remove-deprecated-rules/verify-deprecated-rule-is-removed.html").build()
    );
  }

}
