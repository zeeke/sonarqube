/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.performance.Counters;
import com.sonar.performance.MavenLogs;
import com.sonar.performance.PerformanceTask;

public class MavenScanStruts extends PerformanceTask {

  private final String[] args;

  public MavenScanStruts(String name, String... args) {
    super(name);
    this.args = args;
  }

  @Override
  public int replay() {
    return 3;
  }

  public void execute(Orchestrator orchestrator, Counters counters) {
    MavenBuild build = newBuild();
    build.setGoals("sonar:sonar --offline -V");
    build.setEnvironmentVariable("MAVEN_OPTS", "-Xmx1024m");
    build.setProperties(args);
    BuildResult result = orchestrator.executeBuild(build);

    counters
      .set("Time (ms)", MavenLogs.extractTotalTime(result.getLogs()))
      .set("End Memory (Mb)", MavenLogs.extractEndMemory(result.getLogs()))
      .set("Max Memory (Mb)", MavenLogs.extractMaxMemory(result.getLogs()));
  }

  static MavenBuild newBuild() {
    FileLocation strutsHome = FileLocation.ofShared("it-sonar-performancing/struts-1.3.9/pom.xml");
    return MavenBuild.create(strutsHome.getFile());
  }


}
