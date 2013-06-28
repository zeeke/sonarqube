package org.sonar.samples;

import java.util.List;

import org.sonar.api.SonarPlugin;

import com.google.common.collect.Lists;

public final class SeveralProfilesPlugin extends SonarPlugin {

  @SuppressWarnings({"rawtypes", "unchecked"})
  public List getExtensions() {
    return Lists.newArrayList(FooLanguage.class, Profile1.class, Profile2.class, ModifiableProfile.class);
  }
}
