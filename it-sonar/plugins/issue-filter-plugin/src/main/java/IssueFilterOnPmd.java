import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;

/**
 * This filter removes the issues raised by PMD.
 */
public class IssueFilterOnPmd implements IssueFilter {

  private final Settings settings;

  public IssueFilterOnPmd(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean accept(Issue issue, IssueFilterChain chain) {
    if (settings.getBoolean("enableIssueFilters") && "pmd".equals(issue.ruleKey().repository())) {
      return false;
    }
    return chain.accept(issue);
  }
}
