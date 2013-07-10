package org.sonar.samples;

import org.slf4j.LoggerFactory;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;

@DependedUpon(DecoratorBarriers.ISSUES_ADDED)
public class OneIssuePerLineDecorator implements Decorator {

  public static final RuleKey RULE_KEY = RuleKey.of("deprecated-repo", "deprecated-rule");

  private final ResourcePerspectives perspectives;

  public OneIssuePerLineDecorator(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
  }

  @DependsUpon
  public Metric dependsUponLinesMeasure() {
    // UGLY - this method is marked as unused by IDE
    return CoreMetrics.LINES;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext decoratorContext) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null && ResourceUtils.isFile(resource)) {
      Measure linesMeasure = decoratorContext.getMeasure(CoreMetrics.LINES);
      if (linesMeasure == null) {
        LoggerFactory.getLogger(getClass()).warn("Missing measure " + CoreMetrics.LINES_KEY + " on " + issuable.component());
      } else {
        for (int line = 1; line <= linesMeasure.getValue().intValue(); line++) {
          issuable.addIssue(issuable.newIssueBuilder()
            .line(line)
            .ruleKey(RULE_KEY)
            .message("This issue is generated on each line (Deprecated rule)")
            .build());
        }
      }
    }
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    // UGLY - duplicated in all sensors/decorators
    return "xoo".equals(project.getLanguageKey());
  }
}
