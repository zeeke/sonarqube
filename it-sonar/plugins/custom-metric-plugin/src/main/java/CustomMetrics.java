import org.sonar.api.measures.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class CustomMetrics extends AbstractMetrics {

  public static final Metric CUSTOM = new Metric.Builder("custom", "Custom", Metric.ValueType.FLOAT)
      .setFormula(new CustomFormula())
      .setDomain(CoreMetrics.DOMAIN_GENERAL)
      .setDescription("Custom metric")
      .setQualitative(false)
      .create();

  public List<Metric> getMetrics() {
    return Arrays.asList(CUSTOM);
  }

  /**
   * 2.5 * child values
   */
  private static class CustomFormula implements Formula {
    public List<Metric> dependsUponMetrics() {
      return Collections.emptyList();
    }

    public Measure calculate(FormulaData formulaData, FormulaContext formulaContext) {
      Collection<Measure> children = formulaData.getChildrenMeasures(CUSTOM);
      double result = 0.0;
      for (Measure child : children) {
        if (child.getValue() != null) {
          result += 2.5 * child.getValue();
        }
      }
      return new Measure(CUSTOM).setValue(result, 2);
    }
  }
}
