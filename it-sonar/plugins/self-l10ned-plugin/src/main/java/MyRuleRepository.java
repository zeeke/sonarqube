import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import java.io.InputStream;
import java.util.List;

public final class MyRuleRepository extends RuleRepository {

  private XMLRuleParser xmlRuleParser;

  public MyRuleRepository(XMLRuleParser xmlRuleParser) {
    super("myrepo", Java.KEY);
    setName("My Repository");
    this.xmlRuleParser = xmlRuleParser;
  }

  @Override
  public List<Rule> createRules() {
    InputStream input = getClass().getResourceAsStream("/myrules.xml");
    try {
      return xmlRuleParser.parse(input);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
