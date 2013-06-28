package cobol;

import org.sonar.api.resources.Language;

public class CobolLanguage implements Language {
  public String getKey() {
    return "cobol";
  }

  public String getName() {
    return "cobol";
  }

  public String[] getFileSuffixes() {
    return new String[]{".cbl"};
  }
}
