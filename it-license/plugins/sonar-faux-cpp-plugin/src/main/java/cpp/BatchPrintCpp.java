package cpp;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

public class BatchPrintCpp implements Sensor {
  public void analyse(Project project, SensorContext sensorContext) {
    System.out.println("-- CPP ENABLED --");
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }
}
