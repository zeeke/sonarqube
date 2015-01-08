/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.performance.PerfTestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class PreviewModeTest extends PerfTestCase {

  @ClassRule
  public static Orchestrator orchestrator = BatchPerfTestSuite.ORCHESTRATOR;

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void preview_scan_xoo_project() {
    SonarRunner runner = newSonarRunner(
      "-Xmx512m -server -XX:MaxPermSize=64m",
      "sonar.profile", "one-xoo-issue-per-line",
      "sonar.dryRun", "true",
      "sonar.showProfiling", "true"
      );
    long start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long firstDuration = System.currentTimeMillis() - start;
    System.out.println("First preview analysis: " + firstDuration + "ms");

    // caches are warmed
    start = System.currentTimeMillis();
    orchestrator.executeBuild(runner);
    long secondDuration = System.currentTimeMillis() - start;
    System.out.println("Second preview analysis: " + secondDuration + "ms");

    assertDurationAround(secondDuration, 13200L);
  }

}
