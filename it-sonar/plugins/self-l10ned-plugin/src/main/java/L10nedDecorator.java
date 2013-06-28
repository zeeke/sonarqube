
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.i18n.I18n;
import org.sonar.api.i18n.RuleI18n;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import java.util.Locale;

public final class L10nedDecorator implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(L10nedDecorator.class);

  private I18n i18n;
  private RuleI18n ruleI18n;

  public L10nedDecorator(I18n i18n, RuleI18n ruleI18n) {
    this.i18n = i18n;
    this.ruleI18n = ruleI18n;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @SuppressWarnings("rawtypes")
  public void decorate(Resource resource, DecoratorContext context) {
    if (ResourceUtils.isRootProject(resource)) {
      LOG.info("=====================");
      // Displays: "Ceci est un message"
      LOG.info("> " + i18n.message(Locale.FRANCE, "selfl10ned.message", "notfound"));
      // and the rule name "Ma règle"
      LOG.info("> " + ruleI18n.getName("myrepo", "myrule", Locale.FRANCE));
      // and the rule description "<p>Description HTML de la règle myrule</p>"
      LOG.info("> " + ruleI18n.getDescription("myrepo", "myrule", Locale.FRANCE));
      LOG.info("=====================");
    }
  }
}
