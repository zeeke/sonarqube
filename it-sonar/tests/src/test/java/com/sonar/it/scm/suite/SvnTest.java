/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.scm.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.MapAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static com.sonar.it.scm.suite.ScmTestSuite.PROJECTS_DIR;
import static com.sonar.it.scm.suite.ScmTestSuite.measure;
import static com.sonar.it.scm.suite.ScmTestSuite.project;
import static com.sonar.it.scm.suite.ScmTestSuite.runSonar;
import static com.sonar.it.scm.suite.ScmTestSuite.unzip;
import static org.fest.assertions.Assertions.assertThat;

public class SvnTest {

  @ClassRule
  public static Orchestrator orchestrator = ScmTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void deleteData() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/xoo/one-issue-per-line.xml"));
  }

  @Test
  public void sample_svn_project() {
    unzip("repo-svn.zip");

    String scmUrl = "scm:svn:file:///" + unixPath(project("repo-svn/dummy-svn"));
    checkout("dummy-svn", scmUrl);

    runSonar("dummy-svn");

    assertThat(measure("dummy:dummy:src/main/java/org/dummy/Dummy.java", "authors_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "dgageot"),
        MapAssert.entry("2", "dgageot"),
        MapAssert.entry("3", "dgageot"),
        MapAssert.entry("24", "dgageot"));

    assertThat(measure("dummy:dummy:src/main/java/org/dummy/Dummy.java", "revisions_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "2"),
        MapAssert.entry("2", "2"),
        MapAssert.entry("3", "2"));

    assertThat(measure("dummy:dummy:src/main/java/org/dummy/Dummy.java", "last_commit_datetimes_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "2012-07-19T11:44:57+0200"),
        MapAssert.entry("2", "2012-07-19T11:44:57+0200"),
        MapAssert.entry("3", "2012-07-19T11:44:57+0200"));
  }

  // SONAR-5843
  @Test
  public void sample_svn_project_with_merge() {
    unzip("repo-svn-with-merge.zip");

    String scmUrl = "scm:svn:file:///" + unixPath(project("repo-svn/dummy-svn/trunk"));
    checkout("dummy-svn", scmUrl);

    runSonar("dummy-svn", "sonar.svn.use_merge_history", "true");

    assertThat(measure("dummy:dummy:src/main/java/org/dummy/Dummy.java", "authors_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "dgageot"),
        MapAssert.entry("2", "henryju"),
        MapAssert.entry("3", "dgageot"),
        MapAssert.entry("24", "henryju"));

    assertThat(measure("dummy:dummy:src/main/java/org/dummy/Dummy.java", "revisions_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "2"),
        MapAssert.entry("2", "6"),
        MapAssert.entry("3", "2"));

    assertThat(measure("dummy:dummy:src/main/java/org/dummy/Dummy.java", "last_commit_datetimes_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "2012-07-19T11:44:57+0200"),
        MapAssert.entry("2", "2014-11-06T09:23:04+0100"),
        MapAssert.entry("3", "2012-07-19T11:44:57+0200"));
  }

  private static String unixPath(File file) {
    return file.getAbsolutePath().replace('\\', '/');
  }

  private static void checkout(String destination, String url) {
    orchestrator.executeBuilds(MavenBuild.create()
      .setGoals("org.apache.maven.plugins:maven-scm-plugin:1.7:checkout")
      .setProperty("connectionUrl", url)
      .setProperty("checkoutDirectory", destination)
      .setExecutionDir(PROJECTS_DIR)
      );
  }

}
