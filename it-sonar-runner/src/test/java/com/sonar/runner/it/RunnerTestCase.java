/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.runner.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public abstract class RunnerTestCase {
  @ClassRule
  public static Orchestrator orchestrator = SonarRunnerTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private boolean fork;

  RunnerTestCase(boolean fork) {
    this.fork = fork;
  }

  @Parameterized.Parameters(name = "fork={0}")
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][] { {false}, {true}};
    return Arrays.asList(data);
  }

  SonarRunner newRunner(File baseDir, String... keyValueProperties) {
    SonarRunner runner = SonarRunner.create(baseDir, keyValueProperties);
    runner.setRunnerVersion(Util.runnerVersion(orchestrator).toString());
    if (fork) {
      runner.setProperty("sonarRunner.mode", "fork");
    }
    return runner;
  }

  @Before
  public void assumeVersion22WhenForkMode() {
    if (fork) {
      assumeTrue(Util.runnerVersion(orchestrator).isGreaterThanOrEquals("2.2"));
    }
  }
}
