/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.ClassRule;
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
    .addPlugin(ItUtils.javaPlugin())
    .build();

  @Before
  public void clean() {
    orchestrator.resetData();
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

  private void inspect(String projectPath) {
    MavenBuild inspection = MavenBuild.create(ItUtils.locateProjectPom(projectPath))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setCleanSonarGoals();
    orchestrator.executeBuild(inspection);
  }
}
