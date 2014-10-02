/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.java.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.*;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class Struts139Test {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  private static final String PROJECT_STRUTS = "org.apache.struts:struts-parent";
  private static final String MODULE_CORE = "org.apache.struts:struts-core";
  private static final String PACKAGE_ACTION = "org.apache.struts:struts-core:src/main/java/org/apache/struts/action";
  private static final String FILE_ACTION = "org.apache.struts:struts-core:src/main/java/org/apache/struts/action/Action.java";

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .setGoals("clean", "verify", "sonar:sonar")
      .setProperty("skipTests", "true");
    orchestrator.executeBuild(build);

    MavenBuild analysis = MavenBuild.create(ItUtils.locateProjectPom("shared/struts-1.3.9-diet"))
      .setGoals("sonar:sonar");
    orchestrator.executeBuilds(build, analysis);
  }

  @Test
  public void dependencyTree() {
    List<DependencyTree> trees = orchestrator.getServer().getWsClient().findAll(DependencyTreeQuery.createForProject(PROJECT_STRUTS));
    assertThat(trees.size(), is(0));

    trees = orchestrator.getServer().getWsClient().findAll(DependencyTreeQuery.createForProject(MODULE_CORE));
    assertThat(trees.size(), greaterThan(0));

    assertThat(trees, hasItem(new BaseMatcher<DependencyTree>() {
      public boolean matches(Object o) {
        return StringUtils.equals("antlr:antlr", ((DependencyTree) o).getResourceKey());
      }

      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void dependencies() {
    List<Dependency> dependencies = orchestrator.getServer().getWsClient().findAll(DependencyQuery.createForResource(PROJECT_STRUTS));
    assertThat(dependencies.size(), is(0));

    dependencies = orchestrator.getServer().getWsClient().findAll(DependencyQuery.createForResource(MODULE_CORE));
    assertThat(dependencies.size(), greaterThan(0));
  }

  @Test
  public void versionEvent() {
    EventQuery query = new EventQuery(PROJECT_STRUTS);
    query.setCategories(new String[] {"Version"});
    List<Event> events = orchestrator.getServer().getWsClient().findAll(query);
    assertThat(events.size(), is(1));

    Event version = events.get(0);
    assertThat(version.getName(), is("1.3.9"));
    assertThat(version.getCategory(), is("Version"));
  }

  /**
   * SONAR-2041
   */
  @Test
  public void unknownMetric() {
    assertThat(getProjectMeasure("notfound"), nullValue());
    assertThat(getCoreModuleMeasure("notfound"), nullValue());
    assertThat(getPackageMeasure("notfound"), nullValue());
    assertThat(getFileMeasure("notfound"), nullValue());
  }

  private Measure getFileMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(FILE_ACTION, metricKey)).getMeasure(metricKey);
  }

  private Measure getCoreModuleMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(MODULE_CORE, metricKey)).getMeasure(metricKey);
  }

  private Measure getProjectMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_STRUTS, metricKey)).getMeasure(metricKey);
  }

  private Measure getPackageMeasure(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PACKAGE_ACTION, metricKey)).getMeasure(metricKey);
  }
}
