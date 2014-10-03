/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.scm.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static com.sonar.it.scm.suite.ScmTestSuite.PROJECTS_DIR;
import static com.sonar.it.scm.suite.ScmTestSuite.checkMeasures;
import static com.sonar.it.scm.suite.ScmTestSuite.project;
import static com.sonar.it.scm.suite.ScmTestSuite.runSonar;
import static com.sonar.it.scm.suite.ScmTestSuite.unzip;

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

    checkMeasures("dummy:dummy:src/main/java/org/dummy/Dummy.java");
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
