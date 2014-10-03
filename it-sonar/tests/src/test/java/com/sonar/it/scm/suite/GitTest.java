/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.scm.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static com.sonar.it.scm.suite.ScmTestSuite.checkMeasures;
import static com.sonar.it.scm.suite.ScmTestSuite.runSonar;
import static com.sonar.it.scm.suite.ScmTestSuite.unzip;

public class GitTest {

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
  public void sample_git_project() throws IOException {
    unzip("dummy-git.zip");

    runSonar("dummy-git");

    checkMeasures("dummy-git:dummy:src/main/java/org/dummy/Dummy.java");
  }

}
