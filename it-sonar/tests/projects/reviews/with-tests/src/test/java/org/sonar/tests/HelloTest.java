package org.sonar.tests;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HelloTest {
  @Test
  public void testSay() throws InterruptedException {
    assertEquals("foo", new Hello("foo").say());
  }
}
