package org.sonar.samples;

import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import java.io.InputStream;
import java.util.List;

public final class MissingRuleDescriptionRepository extends RuleRepository {

  private XMLRuleParser xmlRuleParser;

  public MissingRuleDescriptionRepository(XMLRuleParser xmlRuleParser) {
    super("missing-description-repo", Java.KEY);
    setName("Missing description Repo");
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
