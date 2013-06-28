/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BadBatchVersionTest {

  private static final String[] UPGRADABLE_DATABASES = new String[]{"mysql", "mssql", "postgresql", "oracle"};

  /**
   * SONAR-3292
   */
  @Test
  @Ignore("Temporarily disabled")
  public void misleadingMessageWhenBadVersion() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    if (ArrayUtils.contains(UPGRADABLE_DATABASES, builder.getOrchestratorConfiguration().getString("sonar.jdbc.dialect"))) {
      Orchestrator orchestrator = builder.setOrchestratorProperty("sonar.runtimeVersion", "3.0").build();
      orchestrator.start();
      try {
        MavenBuild build = MavenBuild.builder()
          .setPom(ItUtils.locateProjectPom("shared/sample"))
          .addGoal("org.codehaus.sonar:sonar-maven-plugin:" + System.getProperty("sonar.runtimeVersion") + ":sonar")
          .build();

        BuildResult result = orchestrator.executeBuildQuietly(build);

        assertThat(result.getStatus(), is(1));
        assertThat(result.getLogs(), containsString("Database must be upgraded"));
      } finally {
        orchestrator.stop();
      }
    }
  }
}
