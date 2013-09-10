package cpp;

import com.sonarsource.license.api.LicensedPlugin;
import com.sonarsource.license.api.LicensedPluginMetadata;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

import java.util.Arrays;
import java.util.List;

@Properties(
  @Property(name = "License", key = "sonar.cpp.license.secured", type = PropertyType.LICENSE)
)
public final class FakeCppPlugin extends LicensedPlugin {

  @Override
  protected List doGetExtensions() {
    return Arrays.asList(BatchPrintCpp.class, CppLanguage.class, CLanguage.class);
  }

  @Override
  protected LicensedPluginMetadata doGetPluginMetadata() {
    return LicensedPluginMetadata.builder().pluginKey("cpp").licensePropertyKey("sonar.cpp.license.secured").languages("cpp", "c").build();
  }
}
