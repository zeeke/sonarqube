import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class MyTask implements Task {

  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
      .key("my-task")
      .description("A simple task")
      .taskClass(MyTask.class)
      .build();

  private Settings settings;

  public MyTask(Settings settings) {
    this.settings = settings;
  }

  public void execute() {
    LoggerFactory.getLogger(MyTask.class).info("Executing my-task");
    if (settings.getBoolean("sonar.taskCanReadSettings") != true) {
      throw new IllegalStateException("Property not found: sonar.taskCanReadSettings");
    }
  }
}
