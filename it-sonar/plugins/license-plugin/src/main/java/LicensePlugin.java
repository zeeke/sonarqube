import org.sonar.api.*;

import java.util.List;
import java.util.Collections;

@Properties({
    @Property(
        key = "untyped.license.secured",
        name = "Property without license type",
        category = CoreProperties.CATEGORY_GENERAL),
    @Property(
        key = "typed.license.secured",
        name = "Typed property",
        category = CoreProperties.CATEGORY_GENERAL,
        type = PropertyType.LICENSE)
})
public class LicensePlugin extends SonarPlugin {
  public List getExtensions() {
    return Collections.emptyList();
  }
}
