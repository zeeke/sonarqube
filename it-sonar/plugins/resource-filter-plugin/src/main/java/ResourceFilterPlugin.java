import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class ResourceFilterPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(ResourceFilterByFilename.class);
  }
}
