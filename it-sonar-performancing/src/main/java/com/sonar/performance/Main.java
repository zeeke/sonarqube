/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.performance.tasks.*;

import java.util.Arrays;
import java.util.List;

public class Main {

  public static void main(String[] args) throws Exception {
    // Prerequisites
    // - clone the Git repository it-sources and set the env variable $SONAR_IT_SOURCES
    // - if migrations :
    //   -- start database in a version prior or equal to the first target version
    //   -- check that the sonar user "admin" with password "admin" exists
    //   -- check that the quality profile "Sonar way with Findbugs" exists

    new TestPlan()
      .setVersionsOnExistingDb("3.7-SNAPSHOT")
      .setVersionsOnFreshDb("3.5", "3.6.2", "3.7-SNAPSHOT")
      .setTasks(tasks())
      .execute();
  }

  private static List<Task> tasks() {
    return Arrays.asList(
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
  }
}
