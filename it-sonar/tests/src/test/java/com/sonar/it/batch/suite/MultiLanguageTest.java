/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class MultiLanguageTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @After
  public void cleanDatabase() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-926
   * SONAR-5069
   */
  @Test
  public void test_sonar_runner_inspection() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line-xoo2.xml"));
    SonarRunner build = SonarRunner.create().setProjectDir(ItUtils.locateProjectDir("batch/xoo-multi-languages"))
      .setProperty("sonar.profile.xoo", "one-issue-per-line")
      .setProperty("sonar.profile.xoo2", "one-issue-per-line-xoo2");
    BuildResult result = orchestrator.executeBuild(build);

    assertThat(result.getLogs()).contains("2 files indexed");
    assertThat(result.getLogs()).contains("Quality profile for xoo: one-issue-per-line");
    assertThat(result.getLogs()).contains("Quality profile for xoo2: one-issue-per-line-xoo2");

    // modules
    Resource project = getResource("multi-language-sample", "files", "violations");
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(26);

    Resource xooFile = getResource("multi-language-sample:src/sample/Sample.xoo", "violations");
    assertThat(xooFile.getMeasureIntValue("violations")).isEqualTo(13);

    Resource xoo2File = getResource("multi-language-sample:src/sample/Sample.xoo2", "violations");
    assertThat(xoo2File.getMeasureIntValue("violations")).isEqualTo(13);
  }

  /**
   * SONAR-5212
   */
  @Test
  public void test_two_languages_with_tests() {
    MavenBuild maven = MavenBuild.create(ItUtils.locateProjectPom("batch/multi-languages-with-tests"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(maven);

    Resource phpTestDir = getResource("multi-languages:multi-languages-with-tests:src/test/php", "tests");
    assertThat(phpTestDir.getMeasureIntValue("tests")).isEqualTo(3);

    Resource project = getResource("multi-languages:multi-languages-with-tests", "tests");
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(3);

  }

  private Resource getResource(String resourceKey, String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKeys));
  }
}
