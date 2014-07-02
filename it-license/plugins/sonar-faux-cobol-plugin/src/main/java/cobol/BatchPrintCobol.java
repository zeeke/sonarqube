package cobol;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

public class BatchPrintCobol implements Sensor {

  private final ModuleFileSystem fs;

  public BatchPrintCobol(ModuleFileSystem fs) {
    this.fs = fs;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    System.out.println("-- COBOL ENABLED --");
  }

  public boolean shouldExecuteOnProject(Project project) {
    return !fs.files(FileQuery.onSource().onLanguage("cobol")).isEmpty();
  }
}
