import org.junit.Assert;
import org.junit.Test;

public class FailingTest {
  @Test
  public void fail() {
    Assert.fail("Fail on purpose!");
  }
}
