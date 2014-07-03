package sqale;

import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class TaskPrintBip implements Task {

  public static final TaskDefinition DEF = TaskDefinition.builder()
    .description("Print BIP")
    .key("sqale")
    .taskClass(TaskPrintBip.class)
    .build();

  public void execute() {
    System.out.println("-- BIP BIP --");
  }

}
