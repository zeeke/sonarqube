/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildFailureException;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.*;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class BatchTest {

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

  @Test
  public void should_scan_branch() {
    scan("shared/xoo-multi-modules-sample");
    scan("shared/xoo-multi-modules-sample", "sonar.branch", "0.x");

    Sonar sonar = orchestrator.getServer().getWsClient();
    assertThat(sonar.findAll(new ResourceQuery().setQualifiers("TRK"))).hasSize(2);

    Resource master = sonar.find(new ResourceQuery("com.sonarsource.it.samples:multi-modules-sample"));
    assertThat(master.getName()).isEqualTo("Sonar :: Integration Tests :: Multi-modules Sample");

    Resource branch = sonar.find(new ResourceQuery("com.sonarsource.it.samples:multi-modules-sample:0.x"));
    assertThat(branch.getName()).isEqualTo("Sonar :: Integration Tests :: Multi-modules Sample 0.x");
  }

  /**
   * SONAR-2907
   */
  @Test
  public void branch_should_load_own_settings_from_database() {
    scan("shared/xoo-multi-modules-sample");
    assertThat(getResource("com.sonarsource.it.samples:multi-modules-sample:module_b")).isNotNull();

    Sonar sonar = orchestrator.getServer().getAdminWsClient();
    // The parameter skippedModule considers key after first colon
    sonar.update(new PropertyUpdateQuery("sonar.skippedModules", "multi-modules-sample:module_b",
      "com.sonarsource.it.samples:multi-modules-sample"));

    try {
      scan("shared/xoo-multi-modules-sample");
      assertThat(getResource("com.sonarsource.it.samples:multi-modules-sample:module_b")).isNull();

      scan("shared/xoo-multi-modules-sample",
        "sonar.branch", "mybranch");

      assertThat(getResource("com.sonarsource.it.samples:multi-modules-sample:module_b:mybranch")).isNotNull();
    } finally {
      sonar.delete(new PropertyDeleteQuery("sonar.skippedModules", "com.sonarsource.it.samples:multi-modules-sample"));
    }
  }

  // SONAR-4680
  @Test
  public void module_should_load_own_settings_from_database() {
    Sonar sonar = orchestrator.getServer().getAdminWsClient();
    String propKey = "myFakeProperty";
    String rootModuleKey = "com.sonarsource.it.samples:multi-modules-sample";
    String moduleBKey = rootModuleKey + ":module_b";
    sonar.delete(new PropertyDeleteQuery(propKey, rootModuleKey));
    sonar.delete(new PropertyDeleteQuery(propKey, moduleBKey));

    BuildResult result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", "true");

    assertThat(result.getLogs()).excludes(rootModuleKey + ":" + propKey);
    assertThat(result.getLogs()).excludes(moduleBKey + ":" + propKey);

    // Set property only on root project
    sonar.update(new PropertyUpdateQuery(propKey, "project", rootModuleKey));

    result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", "true");

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = project");

    // Override property on moduleB
    sonar.update(new PropertyUpdateQuery(propKey, "moduleB", moduleBKey));

    result = scan("shared/xoo-multi-modules-sample", "sonar.showSettings", "true");

    assertThat(result.getLogs()).contains(rootModuleKey + ":" + propKey + " = project");
    assertThat(result.getLogs()).contains(moduleBKey + ":" + propKey + " = moduleB");
  }

  @Test
  public void should_import_sources_by_default() {
    scan("shared/xoo-sample");

    SourceQuery query = new SourceQuery("sample:src/main/xoo/sample/Sample.xoo");
    Source source = orchestrator.getServer().getWsClient().find(query);
    assertThat(source.getLines()).hasSize(13); // SONAR-3896
    assertThat(source.getLine(3)).isEqualTo("public class Sample {");
  }

  @Test
  public void should_not_import_sources() {
    scan("batch/do-not-import-sources",
      "sonar.importSources", "true");

    Source source = orchestrator.getServer().getWsClient().find(new SourceQuery("do-not-import-sources:src/main/xoo/org/sonar/tests/Hello.xoo"));
    assertThat(source).isNotNull();
    Source testSource = orchestrator.getServer().getWsClient().find(new SourceQuery("do-not-import-sources:src/test/xoo/org/sonar/tests/HelloTest.xoo"));
    assertThat(testSource).isNotNull();

    scan("batch/do-not-import-sources",
      "sonar.importSources", "false");

    source = orchestrator.getServer().getWsClient().find(new SourceQuery("do-not-import-sources:src/main/xoo/org/sonar/tests/Hello.xoo"));
    assertThat(source).isNull();
    testSource = orchestrator.getServer().getWsClient().find(new SourceQuery("do-not-import-sources:src/test/xoo/org/sonar/tests/HelloTest.xoo"));
    assertThat(testSource).isNull();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("do-not-import-sources",
      "/selenium/batch/do-not-import-sources/display-issues-but-not-source.html",
      "/selenium/batch/do-not-import-sources/do-not-display-sources.html",

      // SONAR-2403
      "/selenium/batch/do-not-import-sources/source-of-unit-test.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-2497
   */
  @Test
  public void should_exclude_plugins() {
    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      // exclude xoo plugin
      "sonar.excludePlugins", "xoo",
      "sonar.profile", "");

    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "You must install a plugin that supports the language 'xoo'");
  }

  /**
   * SONAR-3116
   */
  @Test
  public void should_not_exclude_root_module() {
    thrown.expect(BuildFailureException.class);
    scan("shared/xoo-multi-modules-sample",
      "sonar.skippedModules", "multi-modules-sample");
  }

  /**
   * SONAR-3024
   */
  @Test
  public void should_support_source_files_with_same_deprecated_key() {
    scan("batch/duplicate-source");

    Sonar sonar = orchestrator.getServer().getAdminWsClient();
    Resource project = sonar.find(new ResourceQuery("com.sonarsource.it.projects.batch:duplicate-source").setMetrics("files", "directories"));
    // 2 main files and 1 test file all with same deprecated key
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("directories")).isEqualTo(3);
  }

  /**
   * SONAR-3125
   */
  @Test
  public void should_display_explicit_message_when_no_plugin_language_available() {
    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.language", "foo",
      "sonar.profile", "");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "You must install a plugin that supports the language 'foo'");
  }

  @Test
  public void should_display_explicit_message_when_wrong_profile() {
    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.profile", "",
      "sonar.profile.xoo", "unknow");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "Quality profile not found : 'unknow' on language 'xoo'");

    // Old property sonar.profile
    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.profile", "unknow");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains(
      "sonar.profile was set to 'unknow' but didn't match any profile for any language. Please check your configuration.");
  }

  @Test
  public void should_authenticate_when_needed() {
    try {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      BuildResult buildResult = scanQuietly("shared/xoo-sample",
        "sonar.login", "",
        "sonar.password", "");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");

      // SONAR-4048
      buildResult = scanQuietly("shared/xoo-sample",
        "sonar.login", "wrong_login",
        "sonar.password", "wrong_password");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Please check the properties sonar.login and sonar.password.");

      buildResult = scan("shared/xoo-sample",
        "sonar.login", "admin",
        "sonar.password", "admin");
      assertThat(buildResult.getStatus()).isEqualTo(0);

    } finally {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "false"));
    }
  }

  /**
   * SONAR-4211 Test Sonar Runner when server requires authentication
   */
  @Test
  public void sonar_runner_with_secured_server() {
    try {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      BuildResult buildResult = scanQuietly("shared/xoo-sample");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Analyzing this project requires to be authenticated. Please provide the values of the properties sonar.login and sonar.password.");

      buildResult = scanQuietly("shared/xoo-sample",
        "sonar.login", "wrong_login",
        "sonar.password", "wrong_password");
      assertThat(buildResult.getStatus()).isEqualTo(1);
      assertThat(buildResult.getLogs()).contains(
        "Not authorized. Please check the properties sonar.login and sonar.password.");

      buildResult = scan("shared/xoo-sample",
        "sonar.login", "admin",
        "sonar.password", "admin");
      assertThat(buildResult.getStatus()).isEqualTo(0);

    } finally {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "false"));
    }
  }

  /**
   * SONAR-2291
   */
  @Test
  public void batch_should_cache_plugin_jars() throws IOException {
    File userHome = temp.newFolder();

    BuildResult result = scan("shared/xoo-sample",
      "sonar.userHome", userHome.getAbsolutePath());

    File cache = new File(userHome, "cache");
    assertThat(cache).exists().isDirectory();
    int cachedFiles = FileUtils.listFiles(cache, new String[] {"jar"}, true).size();
    assertThat(cachedFiles).isGreaterThan(5);
    assertThat(result.getLogs()).contains("User cache: " + cache.getAbsolutePath());
    assertThat(result.getLogs()).contains("Download sonar-findbugs-plugin-");

    result = scan("shared/xoo-sample",
      "sonar.userHome", userHome.getAbsolutePath());
    assertThat(cachedFiles).isEqualTo(cachedFiles);
    assertThat(result.getLogs()).contains("User cache: " + cache.getAbsolutePath());
    assertThat(result.getLogs()).doesNotContain("Download sonar-findbugs-plugin-");
  }

  /**
   * SONAR-4239
   */
  @Test
  public void should_display_project_url_after_analysis() throws IOException {
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("3.6"));

    BuildResult result = scan("shared/xoo-multi-modules-sample");

    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL, you can browse "
      + orchestrator.getServer().getUrl() + "/dashboard/index/com.sonarsource.it.samples:multi-modules-sample");

    result = scan("shared/xoo-multi-modules-sample",
      "sonar.branch", "mybranch");

    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL, you can browse "
      + orchestrator.getServer().getUrl() + "/dashboard/index/com.sonarsource.it.samples:multi-modules-sample:mybranch");

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.core.serverBaseURL", "http://foo:123/sonar"));

    result = scan("shared/xoo-multi-modules-sample");

    assertThat(result.getLogs()).contains("ANALYSIS SUCCESSFUL, you can browse "
      + "http://foo:123/sonar/dashboard/index/com.sonarsource.it.samples:multi-modules-sample");
  }

  /**
   * SONAR-4188, SONAR-5178
   */
  @Test
  public void should_display_explicit_message_when_invalid_project_key_or_branch() {
    BuildResult buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", "arg$l:");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"arg$l:\" is not a valid project or module key")
      .contains("Allowed characters");

    // SONAR-4629
    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.projectKey", "12345");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"12345\" is not a valid project or module key")
      .contains("Allowed characters");

    buildResult = scanQuietly("shared/xoo-sample",
      "sonar.branch", "arg$l:");
    assertThat(buildResult.getStatus()).isEqualTo(1);
    assertThat(buildResult.getLogs()).contains("\"arg$l:\" is not a valid branch")
      .contains("Allowed characters");
  }

  /**
   * SONAR-4147
   */
  @Test
  public void should_display_profiling() {
    BuildResult buildResult = scan("shared/xoo-sample",
      "sonar.showProfiling", "true");
    assertThat(buildResult.getLogs()).contains("Initializers execution time");
    assertThat(buildResult.getLogs()).contains("Sensors execution time breakdown");
    assertThat(buildResult.getLogs()).contains("Decorators execution time breakdown");
    assertThat(buildResult.getLogs()).contains("Post-Jobs execution time breakdown");
    // Purge
    assertThat(buildResult.getLogs()).contains("Profiling for purge:");
  }

  /**
   * SONAR-4547
   */
  @Test
  public void display_MessageException_without_stacktrace() throws Exception {
    BuildResult result = scanQuietly("shared/xoo-sample", "raiseMessageException", "true");
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs())
      // message
      .contains("Error message from plugin")

      // but not stacktrace
      .doesNotContain("at com.sonarsource.RaiseMessageException");
  }

  /**
   * SONAR-4751
   */
  @Test
  public void file_extensions_are_case_insensitive() throws Exception {
    scan("batch/case-sensitive-file-extensions");

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("case-sensitive-file-extensions", "files", "ncloc"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("ncloc")).isEqualTo(5 + 2);
  }

  /**
   * SONAR-4876
   */
  @Test
  public void custom_module_key() {
    scan("batch/custom-module-key");
    assertThat(getResource("com.sonarsource.it.samples:moduleA")).isNotNull();
    assertThat(getResource("com.sonarsource.it.samples:moduleB")).isNotNull();
  }

  /**
   * SONAR-4692
   */
  @Test
  public void prevent_same_module_key_in_two_projects() {
    scan("batch/prevent-common-module/projectAB");
    assertThat(getResource("com.sonarsource.it.samples:moduleA")).isNotNull();
    assertThat(getResource("com.sonarsource.it.samples:moduleB")).isNotNull();

    BuildResult result = scanQuietly("batch/prevent-common-module/projectAC");
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("Module \"com.sonarsource.it.samples:moduleA\" is already part of project \"projectAB\"");
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines"));
  }

  private BuildResult scan(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuild(runner);
  }

  private BuildResult scanQuietly(String projectPath, String... props) {
    SonarRunner runner = configureRunner(projectPath, props);
    return orchestrator.executeBuildQuietly(runner);
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      .setProfile("one-issue-per-line")
      .setProperties(props);
    return runner;
  }

}
