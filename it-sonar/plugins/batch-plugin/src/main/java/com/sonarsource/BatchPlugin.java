package com.sonarsource;

import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class BatchPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(DumpSettingsInitializer.class, RaiseMessageException.class, TempFolderExtension.class);
  }

}
