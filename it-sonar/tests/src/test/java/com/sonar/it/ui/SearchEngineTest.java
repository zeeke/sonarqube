/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceSearchQuery;
import org.sonar.wsclient.services.ResourceSearchResult;

import static org.fest.assertions.Assertions.assertThat;

public class SearchEngineTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin("java")
    .build();

  @Before
  public void clean() {
    orchestrator.resetData();
  }

  /**
   * Note for Selenium tests :
   * The single command typeKeys does not execute the ajax search request. It must be splitted
   * into two commands type and typeKeys.
   */
  @Test
  @Ignore
  public void testSearchEngine() {
    inspect("shared/struts-1.3.9-diet");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("search-engine",
      "/selenium/ui/search-engine/search-project.html",
      "/selenium/ui/search-engine/search-module.html",
      "/selenium/ui/search-engine/directories-should-not-be-indexed.html",
      "/selenium/ui/search-engine/search-file.html",
      "/selenium/ui/search-engine/search-unknown-file.html",
      "/selenium/ui/search-engine/search-unit-test-file.html",
      "/selenium/ui/search-engine/open-project-dashboard.html",
      "/selenium/ui/search-engine/open-file-viewers.html",
      // SONAR-3909
      "/selenium/ui/search-engine/search-with-percent-and-underscore-characters.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  @Ignore("not stable")
  public void shouldSupportProjectRenaming() {
    inspect("ui/search-engine/project-renaming/before");
    inspect("ui/search-engine/project-renaming/after");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("search-engine-project-renaming",
      "/selenium/ui/search-engine-project-renaming/rename-project.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3946
   */
  @Test
  public void testSearchWebService() {
    inspect("shared/struts-1.3.9-diet");

    Sonar client = orchestrator.getServer().getWsClient();
    ResourceSearchResult result = client.find(ResourceSearchQuery.create("pro").setQualifiers(Resource.QUALIFIER_FILE, Resource.QUALIFIER_PROJECT));

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(10);
    assertThat(result.getTotal()).isEqualTo(11);
    assertThat(result.getResources()).hasSize(10);
    for (ResourceSearchResult.Resource resource : result.getResources()) {
      assertThat(resource.qualifier()).isEqualTo(Resource.QUALIFIER_FILE);
      assertThat(resource.name()).containsIgnoringCase("pro");
      assertThat(resource.key()).startsWith("org.apache.struts:");
    }

    // SONAR-3909
    assertThat(client.find(ResourceSearchQuery.create("pro%").setQualifiers(Resource.QUALIFIER_FILE, Resource.QUALIFIER_PROJECT)).getTotal()).isZero();
    assertThat(client.find(ResourceSearchQuery.create("pro_").setQualifiers(Resource.QUALIFIER_FILE, Resource.QUALIFIER_PROJECT)).getTotal()).isZero();
  }

  /**
   * SONAR-3791
   */
  @Test
  @Ignore
  public void should_support_two_letters_long_projects() throws Exception {
    SonarRunner twoLettersLongProjectScan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-two-letters-named"));
    orchestrator.executeBuild(twoLettersLongProjectScan);

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("search-projects-with-short-name",
        "/selenium/ui/search-engine/search-two-letters-long-project.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  private void inspect(String projectPath) {
    MavenBuild inspection = MavenBuild.create(ItUtils.locateProjectPom(projectPath))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setCleanSonarGoals();
    orchestrator.executeBuild(inspection);
  }
}
