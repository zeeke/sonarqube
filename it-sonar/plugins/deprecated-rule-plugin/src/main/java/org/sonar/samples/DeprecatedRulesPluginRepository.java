package org.sonar.samples;

import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import java.io.InputStream;
import java.util.List;

public final class DeprecatedRulesPluginRepository extends RuleRepository {

  private XMLRuleParser xmlRuleParser;

  public DeprecatedRulesPluginRepository(XMLRuleParser xmlRuleParser) {
    super("deprecated-repo", Java.KEY);
    setName("Deprecated Repo");
    this.xmlRuleParser = xmlRuleParser;
  }

  @Override
  public List<Rule> createRules() {
    InputStream input = getClass().getResourceAsStream("/org/sonar/samples/rules.xml");
    try {
      return xmlRuleParser.parse(input);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
