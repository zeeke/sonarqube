package sqale;

import com.sonarsource.license.api.internal.BatchLicenseVerifier;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;

public class TaskPrintBip implements Task {

  public static final TaskDefinition DEF = TaskDefinition.builder()
    .description("Print BIP")
    .key("sqale")
    .taskClass(TaskPrintBip.class)
    .build();
  private BatchLicenseVerifier batchLicenseVerifier;

  public TaskPrintBip(BatchLicenseVerifier batchLicenseVerifier) {
    this.batchLicenseVerifier = batchLicenseVerifier;
  }

  public void execute() {
    batchLicenseVerifier.verify(true);
    System.out.println("-- BIP BIP --");
  }

}
