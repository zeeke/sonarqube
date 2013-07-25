/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CountersTest {
  @Test
  public void should_keep_lowest_values() throws Exception {
    Counters counters = new Counters();

    counters.set("time", 500L);
    assertThat(counters.values().get("time")).isEqualTo(500L);

    counters.set("time", 800L);
    assertThat(counters.values().get("time")).isEqualTo(500L);

    counters.set("time", 400L);
    assertThat(counters.values().get("time")).isEqualTo(400L);

    counters.set("time", null);
    assertThat(counters.values().get("time")).isEqualTo(400L);
  }
}
