import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;

import java.util.Arrays;
import java.util.List;

public final class SomeXooRulesPluginRepository extends RuleRepository {

  public SomeXooRulesPluginRepository() {
    super("some-rules", "xoo");
    setName("Some rules");
  }

  @Override
  public List<Rule> createRules() {
    return Arrays.asList(
      Rule.create("some-rules", "Rule2", "Rule 2")
    );
  }
}
