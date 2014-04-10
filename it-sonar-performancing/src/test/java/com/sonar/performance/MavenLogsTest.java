/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MavenLogsTest {
  @Test
  public void testExtractTotalTime() throws Exception {
    assertThat(MavenLogs.extractTotalTime("  Total time: 6.015s  ")).isEqualTo(6015);
    assertThat(MavenLogs.extractTotalTime("  Total time: 3:14.025s  ")).isEqualTo(194025);
  }

  @Test
  public void testMaxMemory() throws Exception {
    assertThat(MavenLogs.extractMaxMemory("  Final Memory: 68M/190M  ")).isEqualTo(190);
  }

  @Test
  public void testEndMemory() throws Exception {
    assertThat(MavenLogs.extractEndMemory("  Final Memory: 68M/190M  ")).isEqualTo(68);
  }
}
