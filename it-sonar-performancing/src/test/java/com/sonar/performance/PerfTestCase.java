/*
 * Copyright (C) 2013-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import org.junit.Rule;
import org.junit.rules.TestName;

import static org.fest.assertions.Assertions.assertThat;

public abstract class PerfTestCase {
  private static final double ACCEPTED_DURATION_VARIATION_IN_PERCENTS = 8.0;

  @Rule
  public TestName testName = new TestName();

  void assertDurationAround(long duration, long expectedDuration) {
    double variation = 100.0 * (0.0 + duration - expectedDuration) / expectedDuration;
    System.out.printf("Test %s : executed in %d ms (%.2f %% from target)\n", testName.getMethodName(), duration, variation);
    assertThat(Math.abs(variation)).as(String.format("Expected %d ms, got %d ms", expectedDuration, duration)).isLessThan(ACCEPTED_DURATION_VARIATION_IN_PERCENTS);
  }

  void assertDurationLessThan(long duration, long maxDuration) {
    System.out.printf("Test %s : %d ms (max allowed is %d)\n", testName.getMethodName(), duration, maxDuration);
    assertThat(duration).as(String.format("Expected less than %d ms, got %d ms", maxDuration, duration)).isLessThanOrEqualTo(maxDuration);
  }
}
