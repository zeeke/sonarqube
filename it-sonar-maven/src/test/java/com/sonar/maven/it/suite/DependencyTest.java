/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.internal.HttpRequestFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DependencyTest extends AbstractMavenTest {

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
      .setGoals(cleanPackageSonarGoal());
    orchestrator.executeBuild(build);

    String jsonDeps = post(orchestrator.getServer().adminWsClient(), "/api/dependency_tree", "resource", "com.sonarsource.it.samples:sample-with-deps", "format", "json");
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
      .setGoals(cleanPackageSonarGoal());
    orchestrator.executeBuild(build);

    String jsonDeps = post(orchestrator.getServer().adminWsClient(), "/api/dependency_tree", "resource", "com.sonarsource.it.samples:module_b", "format", "json");
    JSONAssert
      .assertEquals(
        "[{\"w\":1,\"u\":\"compile\",\"s\":\"PRJ\",\"q\":\"BRC\",\"v\":\"1.0-SNAPSHOT\",\"k\":\"com.sonarsource.it.samples:module_a\",\"n\":\"Module A\"},"
          + "{\"w\":1,\"u\":\"test\",\"s\":\"PRJ\",\"q\":\"LIB\",\"v\":\"3.8.1\",\"k\":\"junit:junit\",\"n\":\"junit:junit\"}]",
        jsonDeps, false);
  }

  /**
   * Hack waiting for adminWsClient().post() available in SQ 4.5
   */
  private String post(SonarClient adminWsClient, String relativeUrl, String... params) {
    try {
      Field field = adminWsClient.getClass().getDeclaredField("requestFactory");
      field.setAccessible(true);
      HttpRequestFactory requestFactory = (HttpRequestFactory) field.get(adminWsClient);
      return requestFactory.post(relativeUrl, paramsAsMap(params));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to use reflection on SonarClient", e);
    }
  }

  private Map<String, Object> paramsAsMap(Object[] params) {
    if (params.length % 2 == 1) {
      throw new IllegalArgumentException(String.format(
        "Expecting even number of elements. Got %s", Arrays.toString(params)));
    }
    Map<String, Object> map = new HashMap<String, Object>();
    for (int index = 0; index < params.length; index++) {
      if (params[index] == null) {
        throw new IllegalArgumentException(String.format(
          "Parameter key can't be null at index %d of %s", index, Arrays.toString(params)));
      }
      map.put(params[index].toString(), params[index + 1]);
      index++;
    }
    return map;
  }

}
