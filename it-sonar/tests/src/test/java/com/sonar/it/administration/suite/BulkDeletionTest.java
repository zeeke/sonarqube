/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class BulkDeletionTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-2614, SONAR-3805
   */
  @Test
  public void test_bulk_deletion_on_selected_projects() throws Exception {
    // we must have several projects to test the bulk deletion
    scanCameleonProject("1", "Sample-Project");
    scanCameleonProject("2", "Foo-Application");
    scanCameleonProject("3", "Bar-Sonar-Plugin");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-bulk-deletion-on-selected-project",
      "/selenium/administration/project-bulk-deletion/bulk-delete-filter-projects.html"
      // TODO Rewrite this test through the (future) web service, as there are too many false-positives with the Web part
      // "/selenium/administration/project-bulk-deletion/bulk-delete-selected-projects.html",
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3948
   */
  @Test
  public void test_ghost_deletion() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("crash", "true");
    orchestrator.executeBuildQuietly(build);

    // Check that the project can't be found
    Sonar wsClient = orchestrator.getServer().getAdminWsClient();
    Resource project = wsClient.find(ResourceQuery.create("com.sonarsource.it.samples:simple-sample"));
    assertThat(project).isNull();

    // And clean the corresponding ghost through the bulk deletion service
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ghost-bulk-deletion",
      "/selenium/administration/project-bulk-deletion/bulk-delete-ghosts.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4560
   */
  @Test
  public void should_support_two_letters_long_project_name() throws Exception {
    SonarRunner twoLettersLongProjectScan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-two-letters-named"));
    orchestrator.executeBuild(twoLettersLongProjectScan);

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("bulk-delete-projects-with-short-name",
        "/selenium/administration/project-bulk-deletion/display-two-letters-long-project.html",
        "/selenium/administration/project-bulk-deletion/filter-two-letters-long-project.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  private void scanCameleonProject(String overridenProjectKey, String overridenProjectName) {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("administration/cameleon-project"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("artifactSuffix", overridenProjectKey)
      .setProperty("projectName", overridenProjectName);
    orchestrator.executeBuild(build);
  }

}
