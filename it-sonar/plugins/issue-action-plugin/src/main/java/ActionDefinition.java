import org.sonar.api.ServerExtension;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.condition.HasResolution;

public class ActionDefinition implements ServerExtension {

  private final Actions actions;

  public ActionDefinition(Actions actions) {
    this.actions = actions;
  }

  public void start() {
    actions.add("fake")
      .setConditions(new HasResolution(Issue.RESOLUTION_FIXED))
      .setFunctions(new Function() {
        @Override
        public void execute(Context context) {
          context.setAttribute("fake", "fake action");
          context.addComment("New Comment from fake action");
        }
      });
  }
}
