import org.sonar.api.web.*;

@WidgetProperties({
  @WidgetProperty(key = "mandatoryString", optional = false),
  @WidgetProperty(key = "mandatoryInt", optional = false, type = WidgetPropertyType.INTEGER)

})
public class WidgetWithMandatoryProperties extends AbstractRubyTemplate implements RubyRailsWidget {

  public String getId() {
    return "widget-with-mandatory-properties";
  }

  public String getTitle() {
    return "Widget with Mandatory Properties";
  }

  @Override
  protected String getTemplatePath() {
    return "/widgets/widget-with-mandatory-properties.html.erb";
  }
}

