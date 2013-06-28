package org.sonar.samples;

import java.util.List;

import org.sonar.api.resources.Java;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;

import com.google.common.collect.Lists;

public final class MissingRuleNameRepository extends RuleRepository {

  private AnnotationRuleParser annotationRuleParser;

  public MissingRuleNameRepository(AnnotationRuleParser annotationRuleParser) {
    super("missing-name-repo", Java.KEY);
    setName("Missing name Repo");
    this.annotationRuleParser = annotationRuleParser;
  }

  @Override
  public List<Rule> createRules() {
    return annotationRuleParser.parse("missing-name-repo", Lists.newArrayList((Class) FakeRule.class));
  }
}
