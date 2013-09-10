package cpp;

import org.sonar.api.resources.Language;

public class CLanguage implements Language {
  public String getKey() {
    return "c";
  }

  public String getName() {
    return "C";
  }

  public String[] getFileSuffixes() {
    return new String[]{".c", ".h"};
  }
}
