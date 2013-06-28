import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public final class PageDecorationPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(FakePageDecorations.class);
  }
}
