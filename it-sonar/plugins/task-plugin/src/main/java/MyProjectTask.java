import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class MyProjectTask implements Task {

  public static final TaskDefinition DEFINITION = TaskDefinition.builder()
      .key("my-project-task")
      .description("A simple task that requires a project")
      .taskClass(MyProjectTask.class)
      .build();

  private final ProjectReactor reactor;

  public MyProjectTask(ProjectReactor reactor) {
    this.reactor = reactor;
  }

  public MyProjectTask() {
    this(null);
  }

  public void execute() {
    LoggerFactory.getLogger(MyTask.class).info("Executing my-project-task");
    if (reactor==null || !"multi-languages".equals(reactor.getRoot().getKey())) {
      throw new IllegalStateException("This task was expected to be run on project with key multi-languages");
    }
  }
}
