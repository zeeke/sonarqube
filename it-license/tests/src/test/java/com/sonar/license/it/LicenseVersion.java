/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.license.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.VersionUtils;

class LicenseVersion {
  static String version(Orchestrator orchestrator) {
    return orchestrator.getConfiguration().getString("licenseVersion");
  }

  static boolean isGreaterThanOrEqualTo(Orchestrator orchestrator, String version) {
    return VersionUtils.isGreaterThanOrEqual(version(orchestrator), version);
  }
}
