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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void simple_maven_project() throws JSONException {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/dependencies/sample-with-deps"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    String jsonDeps = orchestrator.getServer().adminWsClient().post("/api/dependency_tree", "resource", "com.sonarsource.it.samples:sample-with-deps", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"did\":\"5\",\"rid\":\"11\",\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"2.4\",\"k\":\"commons-io:commons-io\",\"n\":\"commons-io:commons-io\"},"
          + "{\"did\":\"7\",\"rid\":\"12\",\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"4.10\",\"k\":\"junit:junit\",\"n\":\"junit:junit\",\"to\":["
          + "{\"did\":\"6\",\"rid\":\"13\",\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"1.1\",\"k\":\"org.hamcrest:hamcrest-core\",\"n\":\"org.hamcrest:hamcrest-core\"}]}]",
        jsonDeps, true);
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
        "[{\"did\":\"2\",\"rid\":\"2\",\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"BRC\",\"v\":\"1.0-SNAPSHOT\",\"k\":\"com.sonarsource.it.samples:module_a\",\"n\":\"Module A\"},"
          + "{\"did\":\"3\",\"rid\":\"6\",\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"3.8.1\",\"k\":\"junit:junit\",\"n\":\"junit:junit\"}]",
        jsonDeps, true);
  }

  // SONAR-5604
  @Test
  public void inject_project_libraries_using_property() throws JSONException {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.maven.projectDependencies", "[{\"k\":\"antlr:antlr\",\"v\":\"2.7.2\",\"s\":\"compile\",\"d\":[]},"
        + "{\"k\":\"commons-fileupload:commons-fileupload\",\"v\":\"1.1.1\",\"s\":\"compile\",\"d\":[{\"k\":\"commons-io:commons-io\",\"v\":\"1.1\",\"s\":\"compile\",\"d\":[]}]},"
        + "{\"k\":\"junit:junit\",\"v\":\"3.8.1\",\"s\":\"test\",\"d\":[]}]");
    orchestrator.executeBuild(build);

    String jsonDeps = orchestrator.getServer().adminWsClient().post("/api/dependency_tree", "resource", "com.sonarsource.it.samples:simple-sample", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"did\":\"1\",\"rid\":\"4\",\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"2.7.2\",\"k\":\"antlr:antlr\",\"n\":\"antlr:antlr\"},"
          + "{\"did\":\"2\",\"rid\":\"5\",\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"1.1.1\",\"k\":\"commons-fileupload:commons-fileupload\",\"n\":\"commons-fileupload:commons-fileupload\",\"to\":["
          + "{\"did\":\"3\",\"rid\":\"6\",\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"1.1\",\"k\":\"commons-io:commons-io\",\"n\":\"commons-io:commons-io\"}]},"
          + "{\"did\":\"4\",\"rid\":\"7\",\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"3.8.1\",\"k\":\"junit:junit\",\"n\":\"junit:junit\"}]",
        jsonDeps, true);
  }

}
