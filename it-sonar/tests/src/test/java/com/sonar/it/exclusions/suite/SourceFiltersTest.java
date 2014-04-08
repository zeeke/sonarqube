/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.exclusions.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class SourceFiltersTest {
  static final String PROJECT = "com.sonarsource.it.projects.batch:source-filters";

  @ClassRule
  public static Orchestrator orchestrator = ExclusionsTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void scanProject() {
    orchestrator.getDatabase().truncateInspectionTables();
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/source-filters"))
      .setProperty("sonar.exclusions", "src/main/java/sourceFilters/**/*BeExcluded.java")
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testExclusionProperty() {
    assertThat(getResource(PROJECT + ":sourceFilters.ToBeExcluded"), nullValue());
    assertThat(getMeasure(PROJECT, "files").getIntValue(), is(1));
  }

  private Measure getMeasure(String resourceKey, String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  private Resource getResource(String resourceKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey));
  }
}
