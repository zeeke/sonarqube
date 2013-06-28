/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.runner.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.VersionUtils;

class Util {
  static boolean isRunnerVersionGreaterThan(Orchestrator orchestrator, String minimumVersion) {
    return VersionUtils.isGreaterThanOrEqual(runnerVersion(orchestrator), minimumVersion);
  }

  static String runnerVersion(Orchestrator orchestrator) {
    return orchestrator.getConfiguration().getString("sonarRunner.version");
  }


}
