import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class MassInspectionTest {

  @Rule
  public Orchestrator orchestrator = Orchestrator.builderEnv()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-2.7.xml"))
    .build();

  @Test
  public void inspect_several_projects() {
    File pom = new File("/Users/sbrandhof/projects/commons-i18n/pom.xml");
    for (int i = 0; i < 2000; i++) {
      System.out.println("--------------- loop " + i);
      MavenBuild build = MavenBuild.create(pom).setGoals("sonar:sonar")
        .setProperty("sonar.branch", String.valueOf(i))
        .setProperty("sonar.profile", "sonar-way-2.7")
        .setProperty("sonar.dynamicAnalysis", "false");
      orchestrator.executeBuild(build);

    }

  }
}
