import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

@SupportedEnvironment("maven")
public class MavenExecution implements MavenPluginHandler {

  private final Settings settings;

  public MavenExecution(Settings settings) {
    this.settings = settings;
  }

  @Override
  public String getGroupId() {
    return "org.apache.maven.plugins";
  }

  @Override
  public String getArtifactId() {
    return "maven-help-plugin";
  }

  @Override
  public String getVersion() {
    return "2.2";
  }

  @Override
  public boolean isFixedVersion() {
    return true;
  }

  @Override
  public String[] getGoals() {
    if (settings.getBoolean("showMavenCompilerHelp")) {
      return new String[]{"describe"};
    }
    return new String[0];
  }

  @Override
  public void configure(Project project, MavenPlugin mavenPlugin) {
    // the parameters of the maven-help-plugin
    mavenPlugin.setParameter("groupId", "org.apache.maven.plugins");
    mavenPlugin.setParameter("artifactId", "maven-compiler-plugin");
    mavenPlugin.setParameter("version", "3.1");
  }
}
