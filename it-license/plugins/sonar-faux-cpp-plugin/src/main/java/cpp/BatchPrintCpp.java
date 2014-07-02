package cpp;

import org.sonar.api.scan.filesystem.FileQuery;

import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

public class BatchPrintCpp implements Sensor {
  
  private final ModuleFileSystem fs;

  public BatchPrintCpp(ModuleFileSystem fs) {
    this.fs = fs;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    System.out.println("-- CPP ENABLED --");
  }

  public boolean shouldExecuteOnProject(Project project) {
    return !fs.files(FileQuery.onSource().onLanguage("c")).isEmpty()
      || !fs.files(FileQuery.onSource().onLanguage("cpp")).isEmpty();
  }
}
