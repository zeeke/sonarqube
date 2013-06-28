import org.slf4j.LoggerFactory;
import org.sonar.api.SonarPlugin;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.utils.Logs;

import java.util.Arrays;
import java.util.List;

public class UserHandlerPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(FakeNewUserHandler.class);
  }

  public static class FakeNewUserHandler implements NewUserHandler {
    public void doOnNewUser(Context context) {
      LoggerFactory.getLogger(UserHandlerPlugin.class).info("NEW USER - login=" + context.getLogin() + ", name=" + context.getName());
    }
  }
}

