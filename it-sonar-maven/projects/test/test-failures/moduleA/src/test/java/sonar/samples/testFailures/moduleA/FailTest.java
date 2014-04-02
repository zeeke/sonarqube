package sonar.samples.testFailures.moduleA;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FailTest  {

  @Test
  public void testAWithFailure() {
    assertEquals(true, false);
  }

  @Test
  public void testAWithError() {
    if (true) throw new RuntimeException("Error test");
  }

  @Test
  public void shouldNotFail() {
    fail();
  }

  @Test
  public void testWithSucces() throws InterruptedException {
    assertEquals(3, new ClassA().a());
    assertTrue(true);
    Thread.sleep(1000); // This guarantees that execution time of test will not be too close to zero
  }
}
