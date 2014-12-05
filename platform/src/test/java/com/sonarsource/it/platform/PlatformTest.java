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
import com.sonar.orchestrator.version.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class PlatformTest {

  private static final String COBOL_PROJECT = "sonar.cobol:custom-check";
  private static final String COBOL_FILE = "sonar.cobol:custom-check:src/TC4E3H0.CBL";
  private static final String COBOL_FILE_DEPRECATED_KEY = "sonar.cobol:custom-check:TC4E3H0.CBL";
  private static final String JAVA_STRUTS = "org.apache.struts:struts-parent";
  private static final String JAVA_COLLECTIONS = "commons-collections:commons-collections";
  private static final String JAVA_VIEWS = "views_java";
  private static final String JAVA_COBOL_VIEWS = "views_java_cobol";
  private static final String FLEX_PROJECT = "com.adobe:as3corelib";
  private static final String FLEX_FILE = "com.adobe:as3corelib:src/com/adobe/utils/StringUtil.as";
  private static final String FLEX_FILE_DEPRECATED_KEY = "com.adobe:as3corelib:com/adobe/utils/StringUtil.as";
  private static boolean is_sonar_4_2_or_more;
  private static Orchestrator orchestrator;
  private static Sonar wsClient;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private static Version viewsVersion;

  @BeforeClass
  public static void start() {
    OrchestratorBuilder builder = Orchestrator.builderEnv().removeDistributedPlugins();
    configureProfiles(builder);
    TestUtils.addAllCompatiblePlugins(builder);
    TestUtils.activateLicenses(builder);
    orchestrator = builder.build();
    orchestrator.start();
    viewsVersion = orchestrator.getConfiguration().getPluginVersion("views");
    configureViews();
    is_sonar_4_2_or_more = orchestrator.getServer().version().isGreaterThanOrEquals("4.2");
    if (is_sonar_4_2_or_more) {
      // In SonarQube 4.2 default language is empty to enbale multi-language mode. But all plugins are not yet ready for that.
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.language", "java"));
    }
    inspectProjects();
    wsClient = orchestrator.getServer().getWsClient();
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  private static void inspectProjects() {
    inspect(new File("projects/cobol-project"));
    File root = new File("target/it/maven-projects");
    for (File baseDir : root.listFiles()) {
      // ignore directories like .temp or __MAXOSX
      if (baseDir.isDirectory() && !baseDir.isHidden() && Character.isLetterOrDigit(baseDir.getName().charAt(0))) {
        inspect(baseDir);
      }
    }
    if (viewsVersion.isGreaterThanOrEquals("2.1")) {
      // Refresh views
      SonarRunner runner;
      try {
        runner = SonarRunner.create()
          .setEnvironmentVariable("SONAR_RUNNER_OPTS", TestUtils.BATCH_JVM_OPTS)
          .setProjectDir(temp.newFolder())
          .setTask("views");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      orchestrator.executeBuild(runner);
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
          "<views><vw key='views_java' def='false'><name>Java</name><p>org.apache.struts:struts-parent</p><p>commons-collections:commons-collections</p></vw><vw key='views_java_cobol' def='false'><name>Java+Cobol</name><p>sonar.cobol:custom-check</p><p>commons-collections:commons-collections</p></vw></views>")
      );
  }

  private static void configureProfiles(OrchestratorBuilder builder) {
    builder
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-cobol-IT.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-flex-IT.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/profile-java-IT.xml"));
  }

  private static void inspect(File baseDir) {
    MavenBuild build = MavenBuild.create(new File(baseDir, "pom.xml"))
      .setEnvironmentVariable("MAVEN_OPTS", TestUtils.BATCH_JVM_OPTS)
      .setProperty("sonar.cpd.engine", "sonar")
      .setProfile("IT")
      // following property to not have differences between SonarQube Java version
      .setProperty("sonar.core.codeCoveragePlugin", "jacoco")
      .setProperty("sonar.dynamicAnalysis", "reuseReports")
      .setProperty("sonar.scm.disabled", "true")
      .setGoals("clean org.jacoco:jacoco-maven-plugin:prepare-agent package", "sonar:sonar");
    orchestrator.executeBuild(build);
  }

  // -------------------------------------------------------------------------------------
  // COBOL
  // -------------------------------------------------------------------------------------

  @Test
  public void cobolProjectInfo() {
    assertThat(wsClient.find(new ResourceQuery(COBOL_PROJECT)).getName()).isEqualTo("Test Custom Check");
  }

  @Test
  public void cobolProjectMeasures() {
    assertThat(getMeasure(COBOL_PROJECT, "files").getIntValue()).isEqualTo(1);
  }

  @Test
  public void cobolFileMeasures() {
    assertThat(getMeasure(cobolFileKey(), "violations").getIntValue()).isEqualTo(1);
  }

  @Test
  public void cobolFileIssues() {
    Issues issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().components(cobolFileKey()));
    assertThat(issues.list().size()).isEqualTo(1);

    Issue issue = issues.list().get(0);
    assertThat(issue.ruleKey()).isEqualTo("cobol:com.mycompany.cobol.sample.checks.SampleCheck");
    assertThat(issue.line()).isEqualTo(3);
    assertThat(issue.severity()).isEqualTo("INFO");
    assertThat(issues.rule(issue).name()).isEqualTo("Sample check");
  }

  private String cobolFileKey() {
    return is_sonar_4_2_or_more ? COBOL_FILE : COBOL_FILE_DEPRECATED_KEY;
  }

  // -------------------------------------------------------------------------------------
  // VIEWS
  // -------------------------------------------------------------------------------------

  @Test
  public void viewsInfo() {
    assertThat(wsClient.find(new ResourceQuery(JAVA_STRUTS)).getName()).isEqualTo("Struts");
    assertThat(wsClient.find(new ResourceQuery(JAVA_COLLECTIONS)).getName()).isEqualTo("Commons Collections");
    assertThat(wsClient.find(new ResourceQuery(JAVA_VIEWS)).getName()).isEqualTo("Java");
    assertThat(wsClient.find(new ResourceQuery(JAVA_COBOL_VIEWS)).getName()).isEqualTo("Java+Cobol");
  }

  @Test
  public void viewsLOC() {
    assertThat(wsClient.find(ResourceQuery.createForMetrics(JAVA_STRUTS, "ncloc")).getMeasure("ncloc").getIntValue()).isEqualTo(50080);
    assertThat(wsClient.find(ResourceQuery.createForMetrics(JAVA_COLLECTIONS, "ncloc")).getMeasure("ncloc").getIntValue()).isEqualTo(26558);
    assertThat(wsClient.find(ResourceQuery.createForMetrics(JAVA_VIEWS, "ncloc")).getMeasure("ncloc").getIntValue()).isEqualTo(76638);
  }

  /**
   * class_complexity_distribution has been replaced by file_complexity_distribution in 2.15
   */
  @Test
  public void viewsMeasures() {
    assertThat(getMeasure(JAVA_VIEWS, "ncloc").getIntValue()).isGreaterThan(70000).isLessThan(80000);
    assertThat(getMeasure(JAVA_VIEWS, "lines").getIntValue()).isGreaterThan(179000);
    assertThat(getMeasure(JAVA_VIEWS, "files").getIntValue()).isGreaterThan(700).isLessThan(800);
    assertThat(getMeasure(JAVA_VIEWS, "statements").getIntValue()).isGreaterThan(33000);
    assertThat(getMeasure(JAVA_VIEWS, "classes").getIntValue()).isGreaterThan(900);
    if (!orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      // metric "packages" removed from 4.2
      assertThat(getMeasure(JAVA_VIEWS, "packages").getIntValue()).isGreaterThan(60);
    }
    assertThat(getMeasure(JAVA_VIEWS, "comment_lines_density").getValue()).isGreaterThan(30);
    assertThat(getMeasure(JAVA_VIEWS, "comment_lines").getIntValue()).isGreaterThan(40000);
    assertThat(getMeasure(JAVA_VIEWS, "public_api").getIntValue()).isGreaterThan(7000).isLessThan(10000);
    assertThat(getMeasure(JAVA_VIEWS, "public_undocumented_api").getIntValue()).isGreaterThan(2000);
    assertThat(getMeasure(JAVA_VIEWS, "public_documented_api_density").getValue()).isGreaterThan(60);
    assertThat(getMeasure(JAVA_VIEWS, "duplicated_lines").getIntValue()).isGreaterThan(30000);
    assertThat(getMeasure(JAVA_VIEWS, "duplicated_blocks").getIntValue()).isGreaterThan(1500);
    assertThat(getMeasure(JAVA_VIEWS, "duplicated_files").getIntValue()).isGreaterThan(200);
    assertThat(getMeasure(JAVA_VIEWS, "duplicated_lines_density").getValue()).isGreaterThan(15).isLessThan(20);

    if (orchestrator.getServer().version().isGreaterThanOrEquals("3.3")) {
      // SONAR-3793 and SONAR-3793
      assertThat(getMeasure(JAVA_VIEWS, "complexity").getIntValue()).isGreaterThan(19000);
      assertThat(getMeasure(JAVA_VIEWS, "function_complexity").getValue()).isGreaterThan(2);
      assertThat(getMeasure(JAVA_VIEWS, "function_complexity_distribution").getData()).isNotNull();
      assertThat(getMeasure(JAVA_VIEWS, "class_complexity").getValue()).isGreaterThan(20);
      assertThat(getMeasure(JAVA_VIEWS, "file_complexity").getValue()).isGreaterThan(25);
      assertThat(getMeasure(JAVA_VIEWS, "file_complexity_distribution").getData()).isNotNull();
    } else {
      assertThat(getMeasure(JAVA_VIEWS, "complexity").getIntValue()).isGreaterThan(19000);
      assertThat(getMeasure(JAVA_VIEWS, "function_complexity").getValue()).isGreaterThan(2.0);
      assertThat(getMeasure(JAVA_VIEWS, "function_complexity_distribution").getData()).isNotNull();
      assertThat(getMeasure(JAVA_VIEWS, "class_complexity").getValue()).isGreaterThan(20);
      assertThat(getMeasure(JAVA_VIEWS, "file_complexity").getValue()).isGreaterThan(25);
      assertThat(getMeasure(JAVA_VIEWS, "file_complexity_distribution").getData()).isNotNull();
    }

    assertThat(getMeasure(JAVA_VIEWS, "violations").getIntValue()).isGreaterThan(9000);
    assertThat(getMeasure(JAVA_VIEWS, "weighted_violations").getIntValue()).isGreaterThan(12000);
    assertThat(getMeasure(JAVA_VIEWS, "violations_density").getValue()).isGreaterThan(80.0);

    assertThat(getMeasure(JAVA_VIEWS, "coverage").getValue()).isGreaterThan(30);
    assertThat(getMeasure(JAVA_VIEWS, "tests").getIntValue()).isGreaterThan(13000);
    assertThat(getMeasure(JAVA_VIEWS, "test_success_density").getValue()).isEqualTo(100.0);
    assertThat(getMeasure(JAVA_VIEWS, "test_errors").getIntValue()).isEqualTo(0);

    assertThat(getMeasure(JAVA_VIEWS, "package_tangle_index").getValue()).isGreaterThan(30);
    assertThat(getMeasure(JAVA_VIEWS, "package_feedback_edges").getIntValue()).isGreaterThan(20);
    if (!orchestrator.getServer().version().isGreaterThanOrEquals("4.1")) {
      // SONAR-4853 LCOM4 is no more computed on SQ 4.1
      assertThat(getMeasure(JAVA_VIEWS, "lcom4").getValue()).isEqualTo(1.0);
    }
    if (!orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      // RFC removed from 4.2
      assertThat(getMeasure(JAVA_VIEWS, "rfc").getValue()).isEqualTo(18.7);
    }
  }

  /**
   * SONAR-3177
   */
  @Test
  public void viewsShouldNotAggregateComplexityDistributionForDifferentLanguages() {
    assertThat(getMeasure(JAVA_COBOL_VIEWS, "function_complexity_distribution")).isNull();
    assertThat(getMeasure(JAVA_COBOL_VIEWS, "file_complexity_distribution")).isNull();
  }

  // -------------------------------------------------------------------------------------
  // FLEX
  // -------------------------------------------------------------------------------------

  @Test
  public void flexProjectInfo() {
    assertThat(wsClient.find(new ResourceQuery(FLEX_PROJECT)).getName()).isEqualTo("as3corelib");
  }

  @Test
  public void flexFileSource() {
    assertThat(wsClient.find(new ResourceQuery(is_sonar_4_2_or_more ? FLEX_FILE : FLEX_FILE_DEPRECATED_KEY))).isNotNull();
  }

  private Measure getMeasure(String resourceKey, String metricKey) {
    Resource resource = wsClient.find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    if (resource != null) {
      return resource.getMeasure(metricKey);
    }
    return null;
  }
}
