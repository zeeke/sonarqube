import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;

/**
 * This plugin relates to projects/project-builder sample
 */
public final class RenameProject extends ProjectBuilder {

  public RenameProject(ProjectReactor reactor) {
    super(reactor);
  }

  @Override
  protected void build(ProjectReactor reactor) {
    System.out.println("---> Renaming project");
    // change name of root project
    ProjectDefinition root = reactor.getRoot();
    root.setName("Name changed by plugin");
  }
}
