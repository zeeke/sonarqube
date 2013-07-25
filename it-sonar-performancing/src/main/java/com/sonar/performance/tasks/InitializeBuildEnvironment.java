/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.performance.Counters;
import com.sonar.performance.Task;

public class InitializeBuildEnvironment implements Task {

  @Override
  public void execute(Orchestrator orchestrator, Counters counters) {
    MavenBuild build = MavenScanStruts.newBuild();
    build.setGoals("clean package sonar:sonar -V").setProperty("skipTests", "true").setProperty("sonar.dryRun", "true");
    orchestrator.executeBuild(build);
  }
}
