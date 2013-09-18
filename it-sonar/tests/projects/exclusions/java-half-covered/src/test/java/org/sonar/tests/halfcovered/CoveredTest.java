package org.sonar.tests.halfcovered;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CoveredTest {

  @Test
  public void shouldReturnOne() {
    assertThat(new Covered().returnOne()).isEqualTo(1);
  }
}
