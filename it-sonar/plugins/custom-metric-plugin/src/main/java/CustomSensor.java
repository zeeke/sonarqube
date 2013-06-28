import org.sonar.api.measures.Measure;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;

public class CustomSensor implements Sensor {

  private static final String TOO_LONG = StringUtils.repeat("0123456789", 401);

  private Settings settings;

  public CustomSensor(Settings settings) {
    this.settings = settings;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    sensorContext.saveMeasure(new JavaFile("One"), CustomMetrics.CUSTOM, 1.0);
    sensorContext.saveMeasure(new JavaFile("Two"), CustomMetrics.CUSTOM, 2.0);
    if (settings.getBoolean("sonar.it.failingMeasure")) {
      sensorContext.saveMeasure(new JavaFile("Break"), new Measure(CustomMetrics.CUSTOM).setValue(1.0).setAlertText(TOO_LONG));
    }
  }
}
