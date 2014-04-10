/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.runner.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.version.Version;

class Util {

  static Version runnerVersion(Orchestrator orchestrator) {
    return Version.create(orchestrator.getConfiguration().getString("sonarRunner.version"));
  }

}
