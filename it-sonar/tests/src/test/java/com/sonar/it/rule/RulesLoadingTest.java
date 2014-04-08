/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RulesLoadingTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  /**
   * SONAR-3305, SONAR-3769
   */
  @Test
  public void should_stop_startup_if_rule_description_is_missing_from_xml_file() {
    thrown.expect(IllegalStateException.class);

    orchestrator = Orchestrator.builderEnv().addPlugin(ItUtils.locateTestPlugin("missing-rule-description-plugin")).build();
    orchestrator.start();
  }

  /**
   * SONAR-3343
   */
  @Test
  public void should_stop_startup_if_rule_name_is_missing_from_annotation() {
    thrown.expect(IllegalStateException.class);

    orchestrator = Orchestrator.builderEnv().addPlugin(ItUtils.locateTestPlugin("missing-rule-name-plugin")).build();
    orchestrator.start();
  }
}
