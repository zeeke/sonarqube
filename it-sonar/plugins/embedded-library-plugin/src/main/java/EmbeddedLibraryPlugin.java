import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class EmbeddedLibraryPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(ServerExtensionUsingEmbeddedLibrary.class);
  }
}
