/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.debt;


import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class PluginModelDefinitionTest {

  @Test
  public void server_fail_to_load_with_plugin_definition_model_having_new_characteristics() throws Exception {
    Orchestrator orchestratorWithModelPlugin = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("technical-debt-model-with-new-characteristics-plugin"))
      .build();

    try {
      orchestratorWithModelPlugin.start();
    } catch (Exception e) {
      File logs = orchestratorWithModelPlugin.getServer().getLogs();
      assertThat(FileUtils.readFileToString(logs)).contains("The characteristic : SUB_MAINTAINABILITY cannot be used as it's not available in default characteristics.");
    } finally {
      orchestratorWithModelPlugin.stop();
    }
  }

  @Test
  public void technical_debt_model_loading_should_not_fail_on_removed_rules() throws Exception {
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.xooPlugin())
      .addPlugin(ItUtils.locateTestPlugin("foo-language-plugin"))
      .build();

    try {
      orchestrator.start();

      // Remove foo language plugin with updatecenter web console
      orchestrator.executeSelenese(
        Selenese.builder().setHtmlTestsInClasspath("remove-foo-language-plugin", "/selenium/debt/remove-foo-language-plugin.html").build()
      );
      orchestrator.restartSonar();

      // TODO check requirement on removed rules do not exists anymore

    } finally {
      orchestrator.stop();
    }
  }

}
