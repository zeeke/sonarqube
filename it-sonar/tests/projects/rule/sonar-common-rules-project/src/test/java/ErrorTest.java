import org.junit.Test;

public class ErrorTest {
  @Test
  public void error() throws Exception {
    throw new Exception("Error on purpose!");
  }
}
