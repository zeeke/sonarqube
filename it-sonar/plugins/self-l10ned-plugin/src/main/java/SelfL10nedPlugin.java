import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public final class SelfL10nedPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(L10nedDecorator.class, MyRuleRepository.class);
  }
}
