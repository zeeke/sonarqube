/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.scm.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import org.fest.assertions.MapAssert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static com.sonar.it.scm.suite.ScmTestSuite.measure;
import static com.sonar.it.scm.suite.ScmTestSuite.runSonar;
import static com.sonar.it.scm.suite.ScmTestSuite.unzip;
import static org.fest.assertions.Assertions.assertThat;

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

    assertThat(measure("dummy-git:dummy:src/main/java/org/dummy/Dummy.java", "authors_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "david@gageot.net"),
        MapAssert.entry("2", "david@gageot.net"),
        MapAssert.entry("3", "david@gageot.net"));

    assertThat(measure("dummy-git:dummy:src/main/java/org/dummy/Dummy.java", "revisions_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "6b3aab35a3ea32c1636fee56f996e677653c48ea"),
        MapAssert.entry("2", "6b3aab35a3ea32c1636fee56f996e677653c48ea"),
        MapAssert.entry("3", "6b3aab35a3ea32c1636fee56f996e677653c48ea"));

    assertThat(measure("dummy-git:dummy:src/main/java/org/dummy/Dummy.java", "last_commit_datetimes_by_line").getDataAsMap(";"))
      .includes(
        MapAssert.entry("1", "2012-07-17T16:12:48+0200"),
        MapAssert.entry("2", "2012-07-17T16:12:48+0200"),
        MapAssert.entry("3", "2012-07-17T16:12:48+0200"));
  }

}
