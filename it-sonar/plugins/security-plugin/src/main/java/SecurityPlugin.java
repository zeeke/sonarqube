import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class SecurityPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(FakeRealm.class, FakeAuthenticator.class);
  }

}
