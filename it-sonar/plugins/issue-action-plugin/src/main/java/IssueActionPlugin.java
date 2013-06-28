import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class IssueActionPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(
      ActionDefinition.class
    );
  }

}
