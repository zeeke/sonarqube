/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.tasks.*;

import java.util.Arrays;
import java.util.List;

public class Main {

  public static void main(String[] args) throws Exception {
    new Main().start();
  }

  private Report report = new Report();

  public void start() throws Exception {
    // Prerequisites
    // - clone the Git repository it-sources and set the env variable $SONAR_IT_SOURCES
    // - if migrations :
    //   -- start database in a version prior to the first target version
    //   -- create the sonar user "admin" with password "admin"
    //   -- create the quality profile "Sonar way with Findbugs" and enable all the Findbugs rules


    // migration runs must be executed BEFORE fresh runs, and in ascending order of versions
    migrationRun("3.7-SNAPSHOT");

    freshRun("3.5");
    freshRun("3.6.2");
    freshRun("3.7-SNAPSHOT");

    report.dump();
  }

  private void migrationRun(String sonarVersion) throws Exception {
    report.setCurrentVersion(sonarVersion + " (FULL)");
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .setSonarVersion(sonarVersion)
      .setOrchestratorProperty("orchestrator.keepDatabase", "true")
      .build();
    run(orchestrator);
  }

  private void freshRun(String sonarVersion) throws Exception {
    report.setCurrentVersion(sonarVersion + " (FRESH)");
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .setSonarVersion(sonarVersion)
      .setOrchestratorProperty("orchestrator.keepDatabase", "false")
      .build();
    run(orchestrator);
  }

  private void run(Orchestrator orchestrator) throws Exception {
    List<Task> tasks = Arrays.asList(
      new StartServer("Start Server"),
      new RestartServer("Start server - second time"),

      // Load cache of maven artifacts and build bytecode for Findbugs
      new InitializeBuildEnvironment(),

      // Fix limitation of Orchestrator, which does not support whitespaces in the profile "Sonar way with Findbugs"
      new RenameFindbugsProfile(),

      // Different scans
      new MavenScanStruts("Struts Maven Scan - no unit tests",
        "sonar.dynamicAnalysis", "false"
      ),
      new MavenScanStruts("Struts Dry Maven Scan - no unit tests",
        "sonar.dynamicAnalysis", "false", "sonar.dryRun", "true"
      ),
      new MavenScanStruts("Struts Maven Scan - no unit tests - findbugs",
        "sonar.dynamicAnalysis", "false",
        "sonar.profile", "findbugs-profile"
      ),
      new MavenScanStruts("Struts Maven scan - no unit tests - cross-project duplications",
        "sonar.dynamicAnalysis", "false",
        "sonar.cpd.cross_project", "true"
      ),
      new MavenScanStruts("Struts Maven Scan - unit tests",
        "sonar.dynamicAnalysis", "true"
      ),

      // Global pages
      new RequestUrl("Homepage", "/"),
      new RequestUrl("Quality Profiles", "/profiles"),
      new RequestUrl("All Issues", "/issues/search"),
      new RequestUrl("All Projects", "/all_projects?qualifier=TRK"),
      new RequestUrl("Measures Filter", "/measures"),
      new RequestUrl("Project Measures Filter", "/measures/search?qualifiers[]=TRK"),
      new RequestUrl("File Measures Filter", "/measures/search?qualifiers[]=FIL"),

      // Project pages
      new RequestUrl("Struts Dashboard", "/dashboard/index/org.apache.struts:struts-parent"),
      new RequestUrl("Struts Issues", "/issues/search?componentRoots=org.apache.struts:struts-parent"),
      new RequestUrl("Struts Violations Drilldown", "/drilldown/violations/org.apache.struts:struts-parent"),
      new RequestUrl("Struts Issues Drilldown", "/drilldown/issues/org.apache.struts:struts-parent"),
      new RequestUrl("Struts Measure Drilldown", "/drilldown/measures/org.apache.struts:struts-parent?metric=ncloc"),
      new RequestUrl("Struts Cloud", "/cloud/index/org.apache.struts:struts-parent"),
      new RequestUrl("Struts Hotspots", "/dashboard/index/org.apache.struts:struts-parent?name=Hotspots"),

      // Static pages
      new RequestUrl("sonar.css", "/stylesheets/sonar.css"),
      new RequestUrl("sonar.js", "/javascripts/sonar.js"),

      new StopServer("Stop Server")
    );

    try {
      for (Task task : tasks) {
        Counters counters = new Counters();
        if (task instanceof PerformanceTask) {
          PerformanceTask perfTask = (PerformanceTask) task;
          System.out.println("\n\n************************* " + perfTask.name() + "\n\n");
          for (int i = 0; i < perfTask.replay(); i++) {
            task.execute(orchestrator, counters);
          }
          report.add(perfTask.name(), counters);
        } else {
          task.execute(orchestrator, counters);
        }
      }
    } finally {
      orchestrator.stop();
    }
  }
}
