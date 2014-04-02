/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ProjectDeleteQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CrossProjectDuplicationsTest extends AbstractMavenTest {

  @Before
  public void analyzeProjects() {
    orchestrator.getDatabase().truncateInspectionTables();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("duplications/cross-project/a"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    build = MavenBuild.create(ItUtils.locateProjectPom("duplications/cross-project/b"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    build = MavenBuild.create(ItUtils.locateProjectPom("duplications/cross-project/b"))
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.branch", "branch")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testMeasures() throws Exception {

    Resource project = getResource("com.sonarsource.it.samples.duplications:a");
    assertThat(project, notNullValue());
    assertThat(project.getMeasureIntValue("duplicated_lines"), is(0));

    project = getResource("com.sonarsource.it.samples.duplications:b");
    assertThat(project, notNullValue());
    assertThat(project.getMeasureIntValue("duplicated_lines"), is(10));

    project = getResource("com.sonarsource.it.samples.duplications:b:branch");
    assertThat(project, notNullValue());
    assertThat(project.getMeasureIntValue("duplicated_lines"), is(0));
  }

  @Test
  public void testViewer() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("duplications",
      "/selenium/duplications/cross-project-duplications-viewer.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-3277
  @Test
  public void shouldDisplayMessageInViewerWhenDuplicationsWithDeletedProjectAreFound() throws Exception {
    Sonar adminClient = orchestrator.getServer().getAdminWsClient();
    adminClient.delete(ProjectDeleteQuery.create("com.sonarsource.it.samples.duplications:a"));
    assertThat(adminClient.find(ResourceQuery.create("com.sonarsource.it.samples.duplications:a")), is(nullValue()));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("duplications-and-deleted-project",
      "/selenium/duplications/duplications-with-deleted-project.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "duplicated_lines"));
  }

}
