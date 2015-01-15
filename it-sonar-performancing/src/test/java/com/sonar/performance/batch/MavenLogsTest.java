/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.batch;

import com.google.common.collect.Lists;
import com.sonar.performance.MavenLogs;
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

  @Test
  public void logs_with_different_computations_take_the_last_one() throws Exception {
    assertThat(MavenLogs.extractComputationTotalTime(Lists.newArrayList(" #1 done: 123123123 ms \r\n", "    #2 done: 123456789 ms"))).isEqualTo(123456789L);
  }
}
