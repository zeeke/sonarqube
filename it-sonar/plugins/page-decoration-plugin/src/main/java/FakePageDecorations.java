import org.sonar.api.web.PageDecoration;

public class FakePageDecorations extends PageDecoration {

  @Override
  protected String getTemplatePath() {
    return "/fake_page_decoration.html.erb";
  }
}