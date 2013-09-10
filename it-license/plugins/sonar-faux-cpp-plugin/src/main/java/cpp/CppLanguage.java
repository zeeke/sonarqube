package cpp;

import org.sonar.api.resources.Language;

public class CppLanguage implements Language {
  public String getKey() {
    return "cpp";
  }

  public String getName() {
    return "C++";
  }

  public String[] getFileSuffixes() {
    return new String[]{".cpp"};
  }
}
