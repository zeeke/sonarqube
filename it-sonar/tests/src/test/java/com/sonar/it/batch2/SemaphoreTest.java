/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch2;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class SemaphoreTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.locateTestPlugin("crash-plugin"))
    .build();

  @After
  public void cleanDatabase() {
    orchestrator.getDatabase().truncateInspectionTables();
    // TODO remove it
    orchestrator.getDatabase().truncate("semaphores");
  }

  @Test
  public void shouldNotBeBlockedByAPreviousCrashedAnalysis() {
    SonarRunner build = buildSampleProjectWithCrashActivated();

    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isEqualTo(1);
    assertThat(result.getLogs()).contains("Crash!");

    build = buildSampleProject();

    result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isEqualTo(0);
  }

  @Test
  public void shouldPreventNewAnalysisIfSemaphoreExistsForTheProject() {
    createSemaphore("batch-sample");
    SonarRunner build = buildSampleProject();

    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isEqualTo(1);
    assertThat(result.getLogs()).contains("It looks like an analysis of 'Sample' is already running");
  }

  private SonarRunner buildSampleProject() {
    return buildSampleProject(false);
  }

  private SonarRunner buildSampleProjectWithCrashActivated() {
    return buildSampleProject(true);
  }

  private SonarRunner buildSampleProject(boolean crash) {
    return SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("crash", Boolean.toString(crash));
  }

  private void createSemaphore(String name) {
    String checksum = DigestUtils.md5Hex(name);
    if (!"oracle".equals(orchestrator.getDatabase().getClient().getDialect())) {
      ItUtils.executeUpdate(orchestrator, "INSERT INTO semaphores (name, checksum, created_at, updated_at, locked_at) " +
        "VALUES ('" + name + "', '" + checksum + "', current_timestamp, current_timestamp, current_timestamp)");
    } else {
      ItUtils.executeUpdate(orchestrator, "INSERT INTO semaphores (id, name, checksum, created_at, updated_at, locked_at) " +
        "VALUES (semaphores_seq.NEXTVAL, '" + name + "', '" + checksum + "', current_timestamp, current_timestamp, current_timestamp)");
    }
  }

}
