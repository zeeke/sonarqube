package org.sonar.tests;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HelloTest {
  @Test
  public void testSay() throws InterruptedException {
    assertEquals("foo", new Hello("foo").say());
    Thread.sleep(1000); // This guarantees that execution time of test will not be too close to zero
  }
}
