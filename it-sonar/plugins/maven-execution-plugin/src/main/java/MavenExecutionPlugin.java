import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class MavenExecutionPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(MavenExecutionInitializer.class, MavenExecution.class);
  }
}
