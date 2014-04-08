/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SONAR-2959
 */
public class SqlLogsTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Test
  public void shouldDisableSqlLogsByDefault() {
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"));
    BuildResult result = orchestrator.executeBuild(build);

    assertThat(result.getLogs()).doesNotContain(("Executed SQL:"));
  }

  @Test
  public void enable_sql_logs() {
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
        .setProperty("sonar.log.profilingLevel", "FULL");
    BuildResult result = orchestrator.executeBuild(build);

    assertThat(result.getLogs()).contains("Executed SQL:");
  }
}
