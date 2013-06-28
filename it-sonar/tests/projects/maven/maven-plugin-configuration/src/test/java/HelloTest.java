import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class HelloTest {
  @Test
  public void shouldSayHello() throws InterruptedException {
    assertEquals("hi", new Hello("hi").say());
  }
}
