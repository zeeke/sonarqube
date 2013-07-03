/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class MavenTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void shouldSupportJarWithoutSources() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/project-with-module-without-sources"))
        .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient()
        .find(ResourceQuery.create("com.sonarsource.it.samples.project-with-module-without-sources:project-with-module-without-sources"));
    assertThat(project.getName()).isEqualTo("Project with 1 module without sources");
  }

  /**
   * See SONAR-594
   */
  @Test
  public void shouldSupportJeeProjects() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/jee"))
        .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.jee:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);

    List<Resource> modules = orchestrator.getServer().getWsClient().findAll(ResourceQuery.create("com.sonarsource.it.samples.jee:parent").setDepth(-1).setQualifiers("BRC"));
    assertThat(modules).hasSize(4);
  }

  /**
   * See SONAR-222
   */
  @Test
  public void shouldSupportMavenExtensions() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-extensions"))
        .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-extensions", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);
  }

  /**
   * This test should be splitted. It checks multiple use-cases at the same time : SONAR-518, SONAR-519 and SONAR-593
   */
  @Test
  public void testBadMavenParameters() {
    // should not fail
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-bad-parameters"))
        .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.maven-bad-parameters:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isGreaterThan(0);
  }

  @Test
  public void shouldAnalyzeMultiModules() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/modules-order"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Sonar sonar = orchestrator.getServer().getWsClient();
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:root")).getName()).isEqualTo("Sonar tests - modules order");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:parent")).getName()).isEqualTo("Parent");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_a")).getName()).isEqualTo("Module A");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_a:[default].HelloA")).getName()).isEqualTo("HelloA");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_b")).getName()).isEqualTo("Module B");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_b:[default].HelloB")).getName()).isEqualTo("HelloB");
  }

  /**
   * See SONAR-2735
   */
  @Test
  public void shouldSupportDifferentDeclarationsForModules() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/modules-declaration"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
    Sonar sonar = orchestrator.getServer().getWsClient();

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:root")).getName()).isEqualTo("Root");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_a")).getName()).isEqualTo("Module A");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_a:[default].HelloA")).getName()).isEqualTo("HelloA");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_b")).getName()).isEqualTo("Module B");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_b:[default].HelloB")).getName()).isEqualTo("HelloB");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_c")).getName()).isEqualTo("Module C");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_c:[default].HelloC")).getName()).isEqualTo("HelloC");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_d")).getName()).isEqualTo("Module D");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_d:[default].HelloD")).getName()).isEqualTo("HelloD");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_e")).getName()).isEqualTo("Module E");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_e:[default].HelloE")).getName()).isEqualTo("HelloE");
  }

  /**
   * See SONAR-2896:
   * if Sonar unable to configure cobertura-maven-plugin, then coverage.xml wouldn't be generated
   */
  @Test
  public void testMavenPluginConfiguration() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/maven-plugin-configuration"))
        .setCleanSonarGoals()
        .setProperty("sonar.java.coveragePlugin", "cobertura");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-plugin-configuration", "coverage"));
    assertThat(project.getMeasureIntValue("coverage")).isGreaterThan(0);
  }

  @Test
  public void build_helper_plugin_should_add_dirs_when_dynamic_analysis() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/many-source-dirs"))
        .setCleanPackageSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    checkBuildHelperFiles();
    checkBuildHelperTestFiles();
  }

  /**
   * There was a regression in 2.9 and 2.10, which was fixed in 2.10.1 and 2.11 - SONAR-2744
   */
  @Test
  public void build_helper_plugin_should_add_dirs_when_static_analysis() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/many-source-dirs"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    checkBuildHelperFiles();
  }

  /**
   * See SONAR-3843
   */
  @Test
  public void should_support_shade_with_dependency_reduced_pom_with_clean_install_sonar_goals() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("maven/shade-with-dependency-reduced-pom"))
        .setProperty("sonar.dynamicAnalysis", "false")
        .setGoals("clean", "install", "sonar:sonar");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isEqualTo(0);
    assertThat(result.getLogs()).doesNotContain(
        "Unable to determine structure of project. Probably you use Maven Advanced Reactor Options, which is not supported by Sonar and should not be used.");
  }

  @Test
  public void should_execute_maven_from_scan() throws Exception {
    MavenBuild scan = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
        .setProperty("sonar.dynamicAnalysis", "false")
        .setCleanSonarGoals()

        // See the faux plugin "maven-execution-plugin"
        .setProperty("showMavenCompilerHelp", "true");
    BuildResult result = orchestrator.executeBuild(scan);
    assertThat(result.getLogs()).contains("The Compiler Plugin is used to compile the sources");
  }

  private void checkBuildHelperFiles() {
    Resource project = getResource("com.sonarsource.it.samples:many-source-dirs");
    assertThat(project).isNotNull();
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(getResource("com.sonarsource.it.samples:many-source-dirs:[default].FirstClass")).isNotNull();
    assertThat(getResource("com.sonarsource.it.samples:many-source-dirs:[default].SecondClass")).isNotNull();
  }

  private void checkBuildHelperTestFiles() {
    Resource project = getResource("com.sonarsource.it.samples:many-source-dirs");
    assertThat(project).isNotNull();
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(2);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "files", "tests"));
  }

}
