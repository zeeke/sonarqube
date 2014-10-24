/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.wsclient.services.ResourceQuery;

public class FileDependencyTest {

  @ClassRule
  public static Orchestrator orchestrator = BatchTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @BeforeClass
  public static void prepare() {
    orchestrator.resetData();
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("batch/file-dependencies"));
    orchestrator.executeBuild(build);
  }

  @Test
  public void testDirectoryDependenciesAndDsm() throws JSONException {
    String jsonDeps = orchestrator.getServer().getWsClient().find(new ResourceQuery("sample").setMetrics("dsm")).getMeasure("dsm").getData();
    System.out.println(jsonDeps);
    JSONAssert
      .assertEquals(
        "[{\"n\":\"src/circular\",\"q\":\"DIR\",\"v\":[{},{},{},{}]},"
          + "{\"n\":\"src/crossdirectory1\",\"q\":\"DIR\",\"v\":[{},{},{\"w\":1},{}]},"
          + "{\"n\":\"src/crossdirectory2\",\"q\":\"DIR\",\"v\":[{},{\"w\":2},{},{}]},"
          + "{\"n\":\"src/sample\",\"q\":\"DIR\",\"v\":[{},{},{},{}]}]",
        jsonDeps, false);
  }

  @Test
  public void testFileDependenciesAndDsm() throws JSONException {
    String jsonDeps = orchestrator.getServer().getWsClient().find(new ResourceQuery("sample:src/circular").setMetrics("dsm")).getMeasure("dsm").getData();
    System.out.println(jsonDeps);
    JSONAssert
      .assertEquals(
        "[{\"n\":\"Sample2.xoo\",\"q\":\"FIL\",\"v\":[{},{\"w\":3}]},"
          + "{\"n\":\"Sample.xoo\",\"q\":\"FIL\",\"v\":[{\"w\":5},{}]}]",
        jsonDeps, false);
  }

}
