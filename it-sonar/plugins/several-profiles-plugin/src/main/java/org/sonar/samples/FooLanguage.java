package org.sonar.samples;

import org.sonar.api.resources.AbstractLanguage;

public final class FooLanguage extends AbstractLanguage {

  public FooLanguage() {
    super("foo", "Foo Language");
  }

  public String[] getFileSuffixes() {
    return null;
  }

}
