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
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class TempFolderTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
  }

  // SONAR-4748
  @Test
  public void should_create_in_temp_folder() {
    File projectDir = ItUtils.locateProjectDir("shared/xoo-sample");
    BuildResult result = scan("shared/xoo-sample");

    assertThat(result.getLogs()).excludes("Creating temp directory:");
    assertThat(result.getLogs()).excludes("Creating temp file:");

    result = scan("shared/xoo-sample", "sonar.createTempFiles", "true");
    assertThat(result.getLogs()).contains(
      "Creating temp directory: " + projectDir.getAbsolutePath() + File.separator + ".sonar" + File.separator + ".sonartmp" + File.separator + "sonar-it");
    assertThat(result.getLogs()).contains(
      "Creating temp file: " + projectDir.getAbsolutePath() + File.separator + ".sonar" + File.separator + ".sonartmp" + File.separator + "sonar-it");

    // Verify temp folder is deleted after analysis
    assertThat(new File(projectDir, ".sonar/.sonartmp/sonar-it")).doesNotExist();
  }

  // SONAR-4748
  @Test
  public void should_not_use_system_tmp_dir() throws Exception {
    String oldTmp = System.getProperty("java.io.tmpdir");
    try {
      File tmp = temp.newFolder();
      assertThat(tmp.list()).isEmpty();

      SonarRunner runner = configureRunner("shared/xoo-sample")
        .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Djava.io.tmpdir=" + tmp.getAbsolutePath());
      orchestrator.executeBuild(runner);

      // TODO There is one remaining file waiting for SONARPLUGINS-3185
      assertThat(tmp.list()).hasSize(1);
      assertThat(tmp.list()[0]).matches("sonar-runner-batch(.*).jar");
    } finally {
      System.setProperty("java.io.tmpdir", oldTmp);
    }
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuild(runner);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      // SONARPLUGINS-3771
      .setRunnerVersion("2.3")
      .setProfile("one-issue-per-line")
      .setProperties(props);
    return runner;
  }

}
