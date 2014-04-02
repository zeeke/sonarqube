package sonar.samples.testFailures.moduleB;

import org.junit.Test;
import org.junit.Ignore;

public class SkippedTest {

  @Ignore
  @Test
  public void skippedTest(){}
  
  @Test
  public void normalTest(){}
  
}
