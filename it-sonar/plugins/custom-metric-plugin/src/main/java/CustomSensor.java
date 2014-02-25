import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;

public class CustomSensor implements Sensor {

  private Settings settings;

  public CustomSensor(Settings settings) {
    this.settings = settings;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    if (settings.getBoolean("sonar.it.failingMeasure")) {
      // field is too long for database column -> raise error
      sensorContext.saveMeasure(File.create("src/main/java/One.java"),
        new Measure(CustomMetrics.CUSTOM).setValue(1.0).setAlertText(StringUtils.repeat("0123456789", 1000)));
    } else {
      sensorContext.saveMeasure(File.create("src/main/java/One.java"), CustomMetrics.CUSTOM, 1.0);
      sensorContext.saveMeasure(File.create("src/main/java/Two.java"), CustomMetrics.CUSTOM, 2.0);
    }
  }
}
