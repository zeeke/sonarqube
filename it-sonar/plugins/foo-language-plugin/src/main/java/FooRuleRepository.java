/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;

import java.util.Arrays;
import java.util.List;

public class FooRuleRepository extends RuleRepository {

  public FooRuleRepository() {
    super(FooPlugin.PLUGIN_KEY, FooPlugin.PLUGIN_KEY);
    setName(FooPlugin.PLUGIN_NAME);
  }

  @Override
  public List<Rule> createRules() {
    return Arrays.asList(
      Rule.create(FooPlugin.PLUGIN_KEY, "FooRule1"),
      Rule.create(FooPlugin.PLUGIN_KEY, "FooRule2"),
      Rule.create(FooPlugin.PLUGIN_KEY, "FooRuleNotLinkedToRequirement")
    );
  }
}

