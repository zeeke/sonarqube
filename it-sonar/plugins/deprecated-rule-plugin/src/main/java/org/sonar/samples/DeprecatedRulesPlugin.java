package org.sonar.samples;

import org.sonar.api.SonarPlugin;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public final class DeprecatedRulesPlugin extends SonarPlugin {
  public List getExtensions() {
    return newArrayList(DeprecatedRulesPluginRepository.class);
  }
}
