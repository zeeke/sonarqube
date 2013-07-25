/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.tasks.*;

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
    //migrationRun("3.7-SNAPSHOT");

    //freshRun("3.5");
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
    try {
      report.add("Start Server", StartServer.execute(orchestrator));
      report.add("Start server, second time", RestartServer.execute(orchestrator));

      // Load cache of maven artifacts and build bytecode for Findbugs
      Struts.mavenBuild(orchestrator);

      // Fix limitation of Orchestrator, which does not support whitespaces in the profile "Sonar way with Findbugs"
      RenameFindbugsProfile.execute(orchestrator);

      // Different scans
      report.add("Struts Maven Scan, no unit tests", Struts.mavenScan(orchestrator, "sonar.dynamicAnalysis", "false"));
      report.add("Struts Dry Maven Scan, no unit tests", Struts.mavenScan(orchestrator, "sonar.dynamicAnalysis", "false", "sonar.dryRun", "true"));
      report.add("Struts Maven Scan, no unit tests, findbugs", Struts.mavenScan(orchestrator, "sonar.dynamicAnalysis", "false", "sonar.profile", "findbugs-profile"));
      report.add("Struts Maven scan, no unit tests, cross-project duplications", Struts.mavenScan(orchestrator, "sonar.dynamicAnalysis", "false", "sonar.cpd.cross_project", "true"));
      report.add("Struts Maven Scan, unit tests", Struts.mavenScan(orchestrator, "sonar.dynamicAnalysis", "true"));

      // Global pages
      report.add("Homepage", RequestUrl.get(orchestrator, "/"));
      report.add("Quality Profiles", RequestUrl.get(orchestrator, "/profiles"));
      report.add("All Issues", RequestUrl.get(orchestrator, "/issues/search"));
      report.add("All Projects", RequestUrl.get(orchestrator, "/all_projects?qualifier=TRK"));
      report.add("Measures", RequestUrl.get(orchestrator, "/measures"));
      report.add("Project Measures", RequestUrl.get(orchestrator, "/measures/search?qualifiers[]=TRK"));
      report.add("File Measures", RequestUrl.get(orchestrator, "/measures/search?qualifiers[]=FIL"));

      // Project pages
      report.add("Struts Dashboard", RequestUrl.get(orchestrator, "/dashboard/index/org.apache.struts:struts-parent"));
      report.add("Struts Issues", RequestUrl.get(orchestrator, "/issues/search?componentRoots=org.apache.struts:struts-parent"));
      report.add("Struts Violations Drilldown", RequestUrl.get(orchestrator, "/drilldown/violations/org.apache.struts:struts-parent"));
      report.add("Struts Issues Drilldown", RequestUrl.get(orchestrator, "/drilldown/issues/org.apache.struts:struts-parent"));
      report.add("Struts Measure Drilldown", RequestUrl.get(orchestrator, "/drilldown/measures/org.apache.struts:struts-parent?metric=ncloc"));
      report.add("Struts Cloud", RequestUrl.get(orchestrator, "/cloud/index/org.apache.struts:struts-parent"));
      report.add("Struts Hotspots", RequestUrl.get(orchestrator, "/dashboard/index/org.apache.struts:struts-parent?name=Hotspots"));

      // Static pages
      report.add("sonar.css", RequestUrl.get(orchestrator, "/stylesheets/sonar.css"));
      report.add("sonar.js", RequestUrl.get(orchestrator, "/javascripts/sonar.js"));

      report.add("Stop Server", StopServer.execute(orchestrator));

    } finally {
      orchestrator.stop();
    }
  }
}
