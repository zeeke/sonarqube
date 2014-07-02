package cobol;

import com.sonarsource.license.api.LicensedPlugin;
import com.sonarsource.license.api.LicensedPluginMetadata;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

import java.util.Arrays;
import java.util.List;

@Properties(@Property(name = "License", key = "sonar.cobol.license.secured", type = PropertyType.LICENSE))
public final class FakeCobolPlugin extends LicensedPlugin {

  @Override
  protected List doGetExtensions() {
    return Arrays.asList(BatchPrintCobol.class, CobolLanguage.class);
  }

  @Override
  protected LicensedPluginMetadata doGetPluginMetadata() {
    return LicensedPluginMetadata.builder().pluginKey("cobol").licensePropertyKey("sonar.cobol.license.secured").language("cobol").build();
  }
}
