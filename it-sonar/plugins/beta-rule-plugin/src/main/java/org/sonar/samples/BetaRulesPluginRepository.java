package org.sonar.samples;

import org.sonar.api.resources.Java;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public final class BetaRulesPluginRepository extends RuleRepository {

  private AnnotationRuleParser annotationRuleParser;

  public BetaRulesPluginRepository(AnnotationRuleParser annotationRuleParser) {
    super("beta-repo", Java.KEY);
    setName("Beta Repo");
    this.annotationRuleParser = annotationRuleParser;
  }

  @Override
  public List<Rule> createRules() {
    return annotationRuleParser.parse("beta-repo", newArrayList((Class) BetaRule.class));
  }
}
