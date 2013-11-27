/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
import org.sonar.api.resources.AbstractLanguage;

public class FooLanguage extends AbstractLanguage {

  public FooLanguage() {
    super(FooPlugin.PLUGIN_KEY, FooPlugin.PLUGIN_NAME);
  }

  @Override
  public String[] getFileSuffixes() {
    return new String[0];
  }
}
