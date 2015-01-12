/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.json.JSONException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;

public class DependencyTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void simple_maven_project() throws JSONException {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/dependencies/sample-with-deps"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    String jsonDeps = orchestrator.getServer().adminWsClient().post("/api/dependency_tree", "resource", "com.sonarsource.it.samples:sample-with-deps", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"2.4\",\"k\":\"commons-io:commons-io\",\"n\":\"commons-io:commons-io\"},"
          + "{\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"4.10\",\"k\":\"junit:junit\",\"n\":\"junit:junit\",\"to\":["
          + "{\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"1.1\",\"k\":\"org.hamcrest:hamcrest-core\",\"n\":\"org.hamcrest:hamcrest-core\"}]}]",
        jsonDeps, false);
  }

  @Test
  public void multi_module_deps() throws JSONException {
    // Module B depends on Module A
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/dependencies/multi-modules-with-deps"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    String jsonDeps = orchestrator.getServer().adminWsClient().post("/api/dependency_tree", "resource", "com.sonarsource.it.samples:module_b", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"BRC\",\"v\":\"1.0-SNAPSHOT\",\"k\":\"com.sonarsource.it.samples:module_a\",\"n\":\"Module A\"},"
          + "{\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"3.8.1\",\"k\":\"junit:junit\",\"n\":\"junit:junit\"}]",
        jsonDeps, false);
  }

  // SONAR-1587
  @Test
  @Ignore
  public void multi_module_deps_with_branch() throws JSONException {
    // Module B depends on Module A
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/dependencies/multi-modules-with-deps"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.branch", "myBranch");
    orchestrator.executeBuild(build);

    String jsonDeps = orchestrator.getServer().adminWsClient().post("/api/dependency_tree", "resource", "com.sonarsource.it.samples:module_b:myBranch", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"BRC\",\"v\":\"1.0-SNAPSHOT\",\"k\":\"com.sonarsource.it.samples:module_a:myBranch\",\"n\":\"Module A myBranch\"},"
          + "{\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"3.8.1\",\"k\":\"junit:junit\",\"n\":\"junit:junit\"}]",
        jsonDeps, false);
  }

  // SONAR-5604
  @Test
  public void inject_project_libraries_using_property() throws JSONException {
    // sonar.maven.projectDependencies is overriden in the pom.xml
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/dependencies/sample-with-deps-as-prop"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    String jsonDeps = orchestrator.getServer().adminWsClient().post("/api/dependency_tree", "resource", "com.sonarsource.it.samples:simple-sample", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"2.7.2\",\"k\":\"antlr:antlr\",\"n\":\"antlr:antlr\"},"
          + "{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"1.1.1\",\"k\":\"commons-fileupload:commons-fileupload\",\"n\":\"commons-fileupload:commons-fileupload\",\"to\":["
          + "{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"1.1\",\"k\":\"commons-io:commons-io\",\"n\":\"commons-io:commons-io\"}]},"
          + "{\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"3.8.1\",\"k\":\"junit:junit\",\"n\":\"junit:junit\"}]",
        jsonDeps, false);
  }

}
