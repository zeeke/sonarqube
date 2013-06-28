package sqale;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

public class BatchPrintBip implements Sensor {
  public void analyse(Project project, SensorContext sensorContext) {
    System.out.println("-- BIP BIP --");
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }
}
