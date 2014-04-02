/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.version.Version;
import org.junit.ClassRule;

public abstract class AbstractMavenTest {

  @ClassRule
  public static Orchestrator orchestrator = MavenTestSuite.ORCHESTRATOR;

  protected static String[] cleanInstallSonarGoal() {
    return new String[] {"clean install", sonarGoal()};
  }

  protected static String sonarGoal() {
    return "org.codehaus.mojo:sonar-maven-plugin:" + mojoVersion().toString() + ":sonar";
  }

  protected static String[] cleanSonarGoal() {
    return new String[] {"clean", sonarGoal()};
  }

  protected static String[] cleanPackageSonarGoal() {
    return new String[] {"clean package", sonarGoal()};
  }

  protected static String[] cleanVerifySonarGoal() {
    return new String[] {"clean verify", sonarGoal()};
  }

  protected static Version mojoVersion() {
    return Version.create(orchestrator.getConfiguration().getString("sonarMojo.version"));
  }

}
