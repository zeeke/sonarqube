import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class TaskPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(
        MyTask.class,
        MyTask.DEFINITION,
        MyProjectTask.class,
        MyProjectTask.DEFINITION
    );
  }
}
