import org.sonar.api.resources.Java;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.*;

@ResourceLanguage(Java.KEY)
@NavigationSection(NavigationSection.RESOURCE_TAB)
public class RubyTab extends AbstractRubyTemplate implements RubyRailsPage {

  public String getId() {
    return getClass().getName();
  }

  public String getTitle() {
    return "Ruby tab";
  }

  @Override
  public String getTemplatePath() {
    return "/ruby-tab.erb";
  }

}
