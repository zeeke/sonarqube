/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.ant.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.AntBuild;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.ViolationQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class AntTest {

  private static Version antTaskVersion;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static Orchestrator orchestrator = null;

  @BeforeClass
  public static void startServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();

    builder.addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-groovy-plugin", "0.6"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-groovy.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-java-classpath.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-java-version.xml"))
      .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/ant/it/profile-project-metadata-java.xml"));

    // SONAR-4358
    // Wating for ORCH-184
    if (Version.create(builder.getSonarVersion()).isGreaterThanOrEquals("4.2")) {
      builder
        .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-cobertura-plugin", "1.5-RC1"))
        // PMD is used by testJavaVersion
        .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1-RC1"));
    } else if (Version.create(builder.getSonarVersion()).isGreaterThanOrEquals("3.7")) {
      // Update to Sonar Java 2.0 in order to allow installation of Cobertura 1.4
      builder.removeDistributedPlugins()
        .setOrchestratorProperty("javaVersion", "2.0")
        .addPlugin("java")
        .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-cobertura-plugin", "1.4"))
        // PMD is used by testJavaVersion
        .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.0"));
    }

    orchestrator = builder.build();
    orchestrator.start();

    antTaskVersion = Version.create(orchestrator.getConfiguration().getString("antTask.version"));
  }

  @AfterClass
  public static void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @After
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  private void buildJava(String project, String target, String profile) {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/" + project + "/build.xml"))
      .setTargets(target, "clean")
      .setProperty("sonar.language", "java")
      .setProperty("sonar.profile", profile);
    orchestrator.executeBuild(build);
  }

  private void buildGroovy(String project, String target, String profile) {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/" + project + "/build.xml"))
      .setTargets(target, "clean")
      .setProperty("sonar.profile", profile);
    orchestrator.executeBuild(build);
  }

  private void checkProjectAnalysed(String projectKey, String profile) {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(projectKey, "profile"));
    assertThat(project.getVersion()).isEqualTo("0.1-SNAPSHOT");
    assertThat(project.getMeasure("profile").getData()).as("Profile").isEqualTo(profile);
  }

  @Test
  public void testProjectMetadata() {
    buildJava("project-metadata", "all", "project-metadata");
    checkProjectAnalysed("org.sonar.ant.tests:project-metadata:1.1.x", "project-metadata");
    Resource project = orchestrator.getServer().getWsClient().find(new ResourceQuery("org.sonar.ant.tests:project-metadata:1.1.x"));
    assertThat(project.getName()).isEqualTo("Ant Project Metadata 1.1.x");
    assertThat(project.getDescription()).isEqualTo("Ant Project with complete metadata");
    assertThat(project.getVersion()).isEqualTo("0.1-SNAPSHOT");
    if (!orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      assertThat(project.getLanguage()).isEqualTo("java");
    }
    assertThat(project.getLongName()).isEqualTo("Ant Project Metadata 1.1.x");
  }

  @Test
  public void testProjectKeyWithoutGroupId() {
    buildJava("project-key-without-groupId", "all", "empty");
    checkProjectAnalysed("project-key-without-groupId", "empty");
  }

  @Test
  public void testClasspath() {
    buildJava("classpath", "all", "classpath");
    checkProjectAnalysed("org.sonar.ant.tests:classpath", "classpath");
    ViolationQuery query = ViolationQuery.createForResource("org.sonar.ant.tests:classpath").setDepth(-1);
    List<Violation> violations = orchestrator.getServer().getWsClient().findAll(query);
    assertThat(violations.size()).isEqualTo(2);
    assertThat(violations).onProperty("ruleKey").contains("squid:CallToDeprecatedMethod");
    assertThat(violations).onProperty("ruleKey").contains("findbugs:DM_EXIT");
  }

  /**
   * This is a test against SONARPLUGINS-1322, which actually was fixed in Sonar 2.11 - see SONAR-2823
   */
  @Test
  public void testSquid() {
    buildJava("squid", "all", "classpath");
    checkProjectAnalysed("org.sonar.ant.tests:squid", "classpath");

    ViolationQuery query = ViolationQuery.createForResource("org.sonar.ant.tests:squid").setDepth(-1);
    List<Violation> violations = orchestrator.getServer().getWsClient().findAll(query);
    assertThat(violations.size()).isEqualTo(1);
    assertThat(violations).onProperty("ruleKey").contains("squid:CallToDeprecatedMethod");
  }

  @Test
  public void testCustomLayout() {
    buildJava("custom-layout", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:custom-layout", "empty");
    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("org.sonar.ant.tests:custom-layout", "packages", "files", "classes", "functions"));
    assertThat(project.getMeasureValue("files")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("classes")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("functions")).isEqualTo(2.0);

    if (!orchestrator.getConfiguration().getSonarVersion().isGreaterThanOrEquals("4.2")) {
      // the metric "packages" is removed in 4.2
      assertThat(project.getMeasureValue("packages")).isEqualTo(1.0);
    }
  }

  @Test
  public void testJavaVersion() {
    buildJava("java-version", "all", "java-version");
    checkProjectAnalysed("org.sonar.ant.tests:java-version", "java-version");
    ViolationQuery query = ViolationQuery.createForResource("org.sonar.ant.tests:java-version").setDepth(-1);
    List<Violation> violations = orchestrator.getServer().getWsClient().findAll(query);
    assertThat(violations.size()).isEqualTo(1);
    assertThat(violations.get(0).getRuleKey()).isEqualTo("pmd:IntegerInstantiation");
  }

  @Test
  public void testJavaWithoutBytecode() {
    buildJava("java-without-bytecode", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:java-without-bytecode", "empty");

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests:java-without-bytecode",
      "lines", "violations",
      "lcom4", "suspect_lcom4_density", "lcom4_distribution",
      "rfc", "rfc_distribution"));

    assertThat(project.getMeasureValue("lines")).isGreaterThan(1.0);
    assertThat(project.getMeasureValue("violations")).isGreaterThanOrEqualTo(0.0);

    // no LCOM4
    assertThat(project.getMeasureValue("lcom4")).isNull();
    assertThat(project.getMeasureValue("suspect_lcom4_density")).isNull();
    assertThat(project.getMeasureValue("lcom4_distribution")).isNull();

    // no RFC
    assertThat(project.getMeasureValue("rfc")).isNull();
    assertThat(project.getMeasureValue("rfc_distribution")).isNull();
  }

  /**
   * SONARPLUGINS-2224
   */
  @Test
  public void testModules() {
    buildJava("modules", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests.modules:root", "empty");

    // Module name
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("org.sonar.ant.tests.modules:root")).getName()).isEqualTo("Project with modules");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("org.sonar.ant.tests.modules:root:one")).getName()).isEqualTo("Module One");

    // Metrics on project
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests.modules:root", "lines", "files"));
    assertThat(project.getMeasureValue("lines")).isEqualTo(10.0);
    assertThat(project.getMeasureValue("files")).isEqualTo(2.0);

    // Metrics on module
    Resource module = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests.modules:root:one", "lines", "files"));
    assertThat(module.getMeasureValue("lines")).isEqualTo(5.0);
    assertThat(module.getMeasureValue("files")).isEqualTo(1.0);

    // Metrics on file
    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests.modules:root:one", "lines"));
    assertThat(file.getMeasureValue("lines")).isEqualTo(5.0);
  }

  /**
   * SONARPLUGINS-1853
   */
  @Test
  public void testModulesWithSpaces() {
    buildJava("modules-with-spaces", "all", "empty");

    checkProjectAnalysed("org.sonar.ant.tests.modules:root", "empty");

    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("org.sonar.ant.tests.modules:root")).getName()).isEqualTo("Project with modules with spaces");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("org.sonar.ant.tests.modules:root:one")).getName()).isEqualTo("Module One");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("org.sonar.ant.tests.modules:root:two")).getName()).isEqualTo("Module Two");
  }

  @Test
  public void testSkippedModules() {
    buildJava("skipped-modules", "all", "empty");

    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("root")).getName()).isEqualTo("Root Module");
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("root:one"))).isNull();
    assertThat(orchestrator.getServer().getWsClient().find(new ResourceQuery("root:two")).getName()).isEqualTo("Module Two");
  }

  @Test
  public void testCobertura() {
    buildJava("cobertura", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:cobertura", "empty");

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests:cobertura",
      "line_coverage", "lines_to_cover", "uncovered_lines",
      "branch_coverage", "conditions_to_cover", "uncovered_conditions",
      "coverage",
      "tests", "test_success_density"));

    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("lines_to_cover")).isEqualTo(4.0);
    assertThat(project.getMeasureValue("uncovered_lines")).isEqualTo(2.0);

    assertThat(project.getMeasureValue("branch_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("conditions_to_cover")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("uncovered_conditions")).isEqualTo(1.0);

    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0);

    assertThat(project.getMeasureValue("tests")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(50.0);
  }

  @Test
  public void testJacoco() {
    buildJava("jacoco", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:jacoco", "empty");

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests:jacoco",
      "line_coverage", "lines_to_cover", "uncovered_lines",
      "branch_coverage", "conditions_to_cover", "uncovered_conditions",
      "coverage",
      "tests", "test_success_density"));

    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("lines_to_cover")).isEqualTo(4.0);
    assertThat(project.getMeasureValue("uncovered_lines")).isEqualTo(2.0);

    assertThat(project.getMeasureValue("branch_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("conditions_to_cover")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("uncovered_conditions")).isEqualTo(1.0);

    assertThat(project.getMeasureValue("coverage")).isEqualTo(50.0);

    assertThat(project.getMeasureValue("tests")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(50.0);
  }

  @Test
  public void testJacocoModules() {
    buildJava("jacoco-modules", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests.jacoco-modules:root", "empty");

    // Metrics on project
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests.jacoco-modules:root",
      "lines",
      "tests", "test_success_density",
      "coverage"));
    assertThat(project.getMeasureValue("lines")).isEqualTo(52.0);
    assertThat(project.getMeasureValue("tests")).isEqualTo(4.0);
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("coverage")).isEqualTo(54.5);

    // Metrics on module
    Resource module = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests.jacoco-modules:root:one",
      "lines",
      "tests", "test_success_density",
      "coverage"));
    assertThat(module.getMeasureValue("lines")).isEqualTo(26.0);
    assertThat(module.getMeasureValue("tests")).isEqualTo(2.0);
    assertThat(module.getMeasureValue("test_success_density")).isEqualTo(50.0);
    assertThat(module.getMeasureValue("coverage")).isEqualTo(54.5);
  }

  @Test
  public void testJacocoTestng() {
    buildJava("testng", "all", "empty");
    checkProjectAnalysed("org.sonar.ant.tests:testng", "empty");

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests:testng",
      "line_coverage", "lines_to_cover", "uncovered_lines",
      "branch_coverage", "conditions_to_cover", "uncovered_conditions",
      "coverage",
      "tests", "test_success_density"));

    assertThat(project.getMeasureValue("line_coverage")).isEqualTo(55.6);
    assertThat(project.getMeasureValue("lines_to_cover")).isEqualTo(9.0);
    assertThat(project.getMeasureValue("uncovered_lines")).isEqualTo(4.0);

    assertThat(project.getMeasureValue("branch_coverage")).isEqualTo(50.0);
    assertThat(project.getMeasureValue("conditions_to_cover")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("uncovered_conditions")).isEqualTo(1.0);

    assertThat(project.getMeasureValue("coverage")).isEqualTo(54.5);

    assertThat(project.getMeasureValue("tests")).isEqualTo(2.0);
    assertThat(project.getMeasureValue("test_success_density")).isEqualTo(50.0);
  }

  @Test
  public void testGroovy() {
    buildGroovy("groovy", "sonar", "groovy");
    checkProjectAnalysed("org.sonar.ant.tests:groovy", "groovy");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("org.sonar.ant.tests:groovy", "ncloc")).getMeasureValue("ncloc")).isGreaterThan(5.0);
  }

  /**
   * SONARPLUGINS-1617
   */
  @Test
  public void testShowSqlLogs() {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/shared/build.xml"))
      .setTargets("all", "clean");
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.1")) {
      // SONAR-4756
      build.setProperty("sonar.log.profilingLevel", "FULL");
    } else {
      build
        .setProperty("sonar.showSql", "true")
        .setProperty("sonar.showSqlResults", "true");
    }
    BuildResult analysisResults = orchestrator.executeBuild(build);

    String logs = analysisResults.getLogs();
    // showSql
    if (orchestrator.getServer().version().isGreaterThanOrEquals("4.2")) {
      // >= sonarqube 4.2 with own SQL profiling
      assertThat(logs).contains("Executed SQL:");
    } else {
      if (orchestrator.getServer().version().isGreaterThanOrEquals("3.2")) {
        // < sonarqube 4.2 and >= sonar 3.2 and mybatis 3.1
        assertThat(logs).contains("==>  Preparing");
      }
      else {
        // < sonar 3.2 and mybatis 3.0
        assertThat(logs).contains("==>  Executing");
      }
      // showSqlResults
      assertThat(logs).contains("<==    Columns");
    }
  }

  /**
   * SONARPLUGINS-2840
   */
  @Test
  public void testVerbose() {
    assumeTrue(antTaskVersion.isGreaterThanOrEquals("2.1"));
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/shared/build.xml"))
      // Workaround for ORCH-174
      .setTargets("all", "clean", "-v");
    BuildResult analysisResults = orchestrator.executeBuild(build);

    String logs = analysisResults.getLogs();
    // verbose
    assertThat(logs).contains("DEBUG - Decorator time:");
  }

  /**
   * SONARPLUGINS-1609 + SONARPLUGINS-1674
   */
  @Test
  public void shouldFailIfMissingProperty() {
    AntBuild build = AntBuild.create()
      .setBuildLocation(FileLocation.of("projects/missing-mandatory-properties/build.xml"))
      .setTargets("sonar");

    BuildResult analysisResults = orchestrator.executeBuildQuietly(build);

    // Do not work on Windows with Orchestrator
    // assertThat(analysisResults.getStatus()).isEqualTo(1));

    String logs = analysisResults.getLogs();
    assertThat(logs).contains("You must define the following mandatory properties");
    assertThat(logs).contains("sonar.projectKey, sonar.projectName, sonar.projectVersion, sonar.sources");
  }

}
