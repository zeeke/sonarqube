package org.sonar.samples;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.SonarPlugin;

public final class RequiredMeasuresWidgetsPlugin extends SonarPlugin {
  @SuppressWarnings({"unchecked", "rawtypes"})
  public List getExtensions() {
    return Arrays.asList(RequiredMeasuresWidgetsDashboard.class,
        WidgetMandatoryAndOneOfSatisfied.class, WidgetMandatoryNotSatisfied.class,
        WidgetMandatorySatisfied.class, WidgetMandatorySatisfiedButNotOneOf.class,
        WidgetNoConstraints.class, WidgetOneOfNotSatisfied.class,
        WidgetOneOfSatisfied.class, WidgetOneOfSatisfiedButNotMandatory.class);
  }
}
