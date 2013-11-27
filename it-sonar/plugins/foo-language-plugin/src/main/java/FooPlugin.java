/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class FooPlugin extends SonarPlugin {

  public static final String PLUGIN_KEY = "foo";
  public static final String PLUGIN_NAME = "Foo";

  @Override
  public List getExtensions() {
    return Arrays.asList(
      FooLanguage.class, FooRuleRepository.class
    );
  }

}
