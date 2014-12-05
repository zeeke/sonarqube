/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ProjectDeleteQuery;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CrossProjectDuplicationsTest {

  @ClassRule
  public static Orchestrator orchestrator = UiTestSuite.ORCHESTRATOR;

  @Before
  public void analyzeProjects() {
    orchestrator.resetData();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("duplications/cross-project/a"))
      .setCleanSonarGoals()
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    build = MavenBuild.create(ItUtils.locateProjectPom("duplications/cross-project/b"))
      .setCleanSonarGoals()
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    build = MavenBuild.create(ItUtils.locateProjectPom("duplications/cross-project/b"))
      .setCleanSonarGoals()
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.branch", "branch")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testViewer() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("duplications-viewer",
      "/selenium/duplications/cross-project-duplications-viewer.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-3277
  @Test
  @Ignore
  public void shouldDisplayMessageInViewerWhenDuplicationsWithDeletedProjectAreFound() throws Exception {
    Sonar adminClient = orchestrator.getServer().getAdminWsClient();
    adminClient.delete(ProjectDeleteQuery.create("com.sonarsource.it.samples.duplications:a"));
    assertThat(adminClient.find(ResourceQuery.create("com.sonarsource.it.samples.duplications:a")), is(nullValue()));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("duplications-and-deleted-project",
      "/selenium/duplications/duplications-with-deleted-project.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

}
