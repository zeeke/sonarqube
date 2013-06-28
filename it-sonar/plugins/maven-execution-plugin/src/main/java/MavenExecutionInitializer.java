import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

public class MavenExecutionInitializer extends Initializer implements DependsUponMavenPlugin {

  private MavenExecution exec;

  public MavenExecutionInitializer(MavenExecution exec) {
    this.exec = exec;
  }

  @Override
  public MavenPluginHandler getMavenPluginHandler(Project project) {
    return exec;
  }

  @Override
  public void execute(Project project) {
  }
}
