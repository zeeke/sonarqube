package cobol;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

public class BatchPrintCobol implements Sensor {
  public void analyse(Project project, SensorContext sensorContext) {
    System.out.println("-- COBOL ENABLED --");
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }
}
