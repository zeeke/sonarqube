import org.junit.Ignore;
import org.junit.Test;

public class IgnoredTest {
  @Test
  @Ignore("On purpose!")
  public void ignore() throws Exception {
  }
}
