package sonar.samples.testFailures.moduleB;

import sonar.samples.testFailures.moduleA.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SuccessTest {

  @Test
  public void testA() throws InterruptedException {
    ClassA a = new ClassA();
    assertEquals(3, a.a());
    Thread.sleep(1000); // This guarantees that execution time of test will not be too close to zero
  }

}
