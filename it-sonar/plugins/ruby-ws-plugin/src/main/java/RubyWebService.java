import org.sonar.api.web.AbstractRubyTemplate;
import org.sonar.api.web.RubyRailsWebservice;

public class RubyWebService extends AbstractRubyTemplate implements RubyRailsWebservice {

  @Override
  public String getTemplatePath() {
    return "/ws/ruby_ws_controller.rb";
  }

  public String getId() {
    return "RubyWebService";
  }

}
