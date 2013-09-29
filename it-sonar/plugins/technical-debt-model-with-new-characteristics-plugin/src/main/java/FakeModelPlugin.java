/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

import org.sonar.api.SonarPlugin;

import java.util.Collections;
import java.util.List;

public final class FakeModelPlugin extends SonarPlugin {

  public List getExtensions() {
    return Collections.emptyList();
  }

}
