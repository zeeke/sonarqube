import org.sonar.api.config.Settings;
import org.sonar.api.rules.Violation;
import org.sonar.api.rules.ViolationFilter;

/**
 * This filter removes the issues raised by PMD.
 * <p/>
 * Violation filters are deprecated in 3.6.
 */
public class ViolationFilterOnPmd implements ViolationFilter {

  private final Settings settings;

  public ViolationFilterOnPmd(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean isIgnored(Violation violation) {
    return settings.getBoolean("enableIssueFilters") && "pmd".equals(violation.getRule().getRepositoryKey());
  }
}
