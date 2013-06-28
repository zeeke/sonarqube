import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class RubyApiTesterPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(RubyApiTesterPage.class);
  }
}
