/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonarsource.it.platform;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class MultipleLangTest {

  private static final String MASTER_PROJECT = "MASTER_PROJECT";
  private static final String MULTI_LANG_PROJECT = "multi-lang-project";
  private static final String STRUTS_PROJECT = "org.apache.struts:struts-parent";
  private static final String PROFILE_NAME = "Default";
  private static final List<String> LANGUAGES = Arrays.asList("abap", "c", "cobol", "cpp", "cs", "flex", "grvy", "java", "js", "natur", "pli", "plsql", "vb", "vbnet");

  private static Orchestrator orchestrator;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @BeforeClass
  public static void start() throws Exception {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    builder.removeDistributedPlugins();
    configureProfiles(builder);
    TestUtils.addAllCompatiblePlugins(builder);
    TestUtils.activateLicenses(builder);
    orchestrator = builder.build();
    orchestrator.start();
    assumeTrue(orchestrator.getConfiguration().getSonarVersion().isGreaterThanOrEquals("4.2"));

    configureViews();
    inspect();
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  private static void configureProfiles(OrchestratorBuilder builder) {
    for (String language : LANGUAGES) {
      builder.restoreProfileAtStartup(FileLocation.ofClasspath("/default-profile-" + language + ".xml"));
    }
  }

  private static void configureViews() {
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("views.status", "D,20100601T07:16:30+0000"));
    orchestrator
      .getServer()
      .getAdminWsClient()
      .update(
        new PropertyUpdateQuery(
          "views.def",
          "<views><vw key='MASTER_PROJECT' def='true'><name>Master Project</name></vw></views>")
      );
  }

  private static void inspect() throws IOException {
    // inspect multi-lang-project
    FileLocation basedir = FileLocation.ofShared("it-sonar-performancing/multi-lang-project/");
    orchestrator.executeBuild(SonarRunner.create()
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-server -Xmx512m -XX:MaxPermSize=160m")
      .setProjectDir(basedir.getFile())
      .setProperty("sonar.sources", "src/main")
      // this name is defined on all languages
      .setProperty("sonar.profile", PROFILE_NAME)
      );

    // inspect Struts (maven)
    MavenBuild maven = MavenBuild.create(FileLocation.ofShared("it-sonar-performancing/struts-1.3.9/pom.xml").getFile())
      .setEnvironmentVariable("MAVEN_OPTS", TestUtils.BATCH_JVM_OPTS)
      .setProperty("sonar.profile", PROFILE_NAME)
      // following property to not have differences between SonarQube Java version
      .setProperty("sonar.core.codeCoveragePlugin", "jacoco")
      .setProperty("sonar.sources", "src/main/java")
      .setProperty("sonar.dynamicAnalysis", "reuseReports")
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent package", "sonar:sonar");
    orchestrator.executeBuild(maven);
  }

  @Test
  public void test_projects() throws Exception {
    // flex: files: 2 ncloc: 12+8 complexity: 2+1
    // groovy: files: 2 ncloc: 11 + 6 complexity: 2+1
    // java: files: 4 ncloc: 9+8+4+3 complexity: 3+1+1+0
    // js: files: 4 ncloc: 42+34+21+10 complexity: 15+13+8+3
    assertThat(getMeasure(MULTI_LANG_PROJECT, "files").getIntValue()).isEqualTo(12);
    assertThat(getMeasure(MULTI_LANG_PROJECT, "ncloc").getIntValue()).isEqualTo(168);
    assertThat(getMeasure(MULTI_LANG_PROJECT, "complexity").getIntValue()).isEqualTo(50);
    assertThat(getMeasure(MULTI_LANG_PROJECT, "coverage").getValue()).isGreaterThan(20.0);
    assertThat(getMeasure(MULTI_LANG_PROJECT, "violations").getIntValue()).isGreaterThan(10);

    assertThat(getMeasure(STRUTS_PROJECT, "files").getIntValue()).isEqualTo(524);
    assertThat(getMeasure(STRUTS_PROJECT, "ncloc").getIntValue()).isEqualTo(53615);
    assertThat(getMeasure(STRUTS_PROJECT, "complexity").getIntValue()).isEqualTo(10943);
    assertThat(getMeasure(STRUTS_PROJECT, "coverage").getValue()).isGreaterThan(14.0);
    assertThat(getMeasure(STRUTS_PROJECT, "violations").getIntValue()).isGreaterThan(100);
  }

  @Test
  public void test_views() throws Exception {
    // consolidate views
    orchestrator.executeBuild(SonarRunner.create().setProjectDir(temp.newFolder()).setTask("views"));

    assertThat(getMeasure(MASTER_PROJECT, "projects").getIntValue()).isEqualTo(2);
    assertThat(getMeasure(MASTER_PROJECT, "files").getIntValue()).isEqualTo(12 + 524);
    assertThat(getMeasure(MASTER_PROJECT, "ncloc").getIntValue()).isEqualTo(168 + 53615);
    assertThat(getMeasure(MASTER_PROJECT, "complexity").getIntValue()).isEqualTo(50 + 10943);
    assertThat(getMeasure(MASTER_PROJECT, "coverage").getValue()).isGreaterThan(10.0);
    assertThat(getMeasure(MASTER_PROJECT, "violations").getIntValue()).isGreaterThan(200);
  }

  @Test
  public void test_developers() throws Exception {
    // consolidate developer cockpit
    orchestrator.executeBuild(SonarRunner.create()
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", TestUtils.BATCH_JVM_OPTS)
      .setProjectDir(temp.newFolder()).setTask("devcockpit"));

    // SB -> projects in it-sources
    assertThat(getMeasure("DEV:simon.brandhof@gmail.com", "ncloc").getIntValue()).isGreaterThan(10);
    assertThat(getMeasure("DEV:simon.brandhof@gmail.com", "violations").getIntValue()).isGreaterThan(0);
  }

  @Test
  public void test_report() throws Exception {
    // generate report
    File basedir = temp.newFolder();
    orchestrator.executeBuild(SonarRunner.create()
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", TestUtils.BATCH_JVM_OPTS)
      .setProjectDir(basedir).setTask("report"));

    // TODO verify that report is correctly generated
    // Requires http://jira.sonarsource.com/browse/REP-43
  }

  private Measure getMeasure(String resourceKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    if (resource != null) {
      return resource.getMeasure(metricKey);
    }
    return null;
  }
}
