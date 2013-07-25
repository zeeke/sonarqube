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

public class Struts {

  public static void mavenBuild(Orchestrator orchestrator) {
    MavenBuild build = newBuild();
    build.setGoals("clean package sonar:sonar -V").setProperty("skipTests", "true").setProperty("sonar.dryRun", "true");
    orchestrator.executeBuild(build);
  }

  public static Counters mavenScan(Orchestrator orchestrator, String... props) {
    MavenBuild build = newBuild();
    build.setGoals("sonar:sonar --offline -V");
    build.setEnvironmentVariable("MAVEN_OPTS", "-Xmx1024m");
    build.setProperties(props);
    BuildResult result = orchestrator.executeBuild(build);

    return new Counters()
      .set("Time", MavenLogs.extractTotalTime(result.getLogs()), "ms")
      .set("End Memory", MavenLogs.extractEndMemory(result.getLogs()), "MB")
      .set("Max Memory", MavenLogs.extractMaxMemory(result.getLogs()), "MB");
  }

  private static MavenBuild newBuild() {
    FileLocation strutsHome = FileLocation.ofShared("it-sonar-performancing/struts-1.3.9/pom.xml");
    return MavenBuild.create(strutsHome.getFile());
  }


}
