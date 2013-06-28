import org.sonar.api.resources.Java;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.*;

@NavigationSection({NavigationSection.HOME})
@UserRole(UserRole.USER)
public class RubyApiTesterPage extends AbstractRubyTemplate implements RubyRailsPage {

  public String getId() {
    return getClass().getName();
  }

  public String getTitle() {
    return "Ruby API Tester";
  }

  @Override
  public String getTemplatePath() {
    return "/ruby-api-tester-page.erb";
  }

}
