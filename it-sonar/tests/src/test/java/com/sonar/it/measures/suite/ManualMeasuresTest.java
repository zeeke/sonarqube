/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.measures.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.ManualMeasureCreateQuery;
import org.sonar.wsclient.services.ManualMeasureDeleteQuery;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.MetricCreateQuery;
import org.sonar.wsclient.services.MetricDeleteQuery;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.MetricUpdateQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ManualMeasuresTest {

  public static final String PROJECT_KEY = "com.sonarsource.it.samples:simple-sample";
  @ClassRule
  public static Orchestrator orchestrator = MeasuresTestSuite.ORCHESTRATOR;

  @Before
  public void deleteProjects() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void manual_measures_should_be_integrated_during_project_analysis() {
    analyzeProject();
    setBurnedBudget(1200.3);
    setTeamSize(4);

    assertThat(getMeasure("team_size")).isNull();
    assertThat(getMeasure("burned_budget")).isNull();

    analyzeProject();

    assertThat(getMeasure("burned_budget").getValue()).isEqualTo(1200.3);
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(4);
  }

  @Test
  public void should_update_value() {
    analyzeProject();
    setTeamSize(4);
    analyzeProject();
    setTeamSize(15);
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(4);
    analyzeProject();// the value is available when the project is analyzed again
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(15);
  }

  @Test
  public void should_delete_manual_measure() {
    analyzeProject();
    deleteManualMeasure("team_size"); // do nothing

    setTeamSize(4);
    analyzeProject();
    deleteManualMeasure("team_size");
    assertThat(getMeasure("team_size").getIntValue()).isEqualTo(4);// the value is still available. It will be removed during next analyzed

    analyzeProject();
    assertThat(getMeasure("team_size")).isNull();
  }

  @Test
  public void test_manual_metrics() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manual-metrics",
      "/selenium/manual-measures/manual-metrics/predefined_metrics.html",
      // SONAR-3792
      "/selenium/manual-measures/manual-metrics/edit_manual_metric_name.html",
      // SONAR-4212
      "/selenium/manual-measures/manual-metrics/edit_provided_manual_metric.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_get_metrics_with_web_service() {
    List<Metric> metrics = orchestrator.getServer().getAdminWsClient().findAll(MetricQuery.all());
    assertThat(metrics).isNotEmpty();

    Metric metric = orchestrator.getServer().getAdminWsClient().find(MetricQuery.byKey("lines"));
    assertThat(metric).isNotNull();
    assertThat(metric.getName()).isEqualTo("Lines");
  }

  /**
   * SONAR-4083
   */
  @Test
  public void should_create_manual_metrics_with_web_service() {
    orchestrator.getServer().getAdminWsClient().create(MetricCreateQuery.create("key_create_test").setName("Metric name").setType("INT").setDomain("domain"));
    Metric metric = getMetric("key_create_test");
    assertThat(metric).isNotNull();
    assertThat(metric.getName()).isEqualTo("Metric name");
    assertThat(metric.getType()).isEqualTo("INT");
    assertThat(metric.getDomain()).isEqualTo("domain");
  }

  /**
   * SONAR-4083
   */
  @Test
  public void should_update_manual_metrics_with_web_service() {
    orchestrator.getServer().getAdminWsClient().create(MetricCreateQuery.create("key_update_test").setName("Metric name").setType("INT").setDomain("domain"));
    orchestrator.getServer().getAdminWsClient().update(MetricUpdateQuery.update("key_update_test").setName("New name").setType("PERCENT").setDomain("New domain"));
    Metric metric = getMetric("key_update_test");
    assertThat(metric.getName()).isEqualTo("New name");
    assertThat(metric.getType()).isEqualTo("PERCENT");
    assertThat(metric.getDomain()).isEqualTo("New domain");
  }

  /**
   * SONAR-4083
   */
  @Test
  public void should_delete_manual_metrics_with_web_service() {
    orchestrator.getServer().getAdminWsClient().create(MetricCreateQuery.create("key_delete_test").setName("Metric name").setType("INT").setDomain("domain"));
    orchestrator.getServer().getAdminWsClient().delete(MetricDeleteQuery.delete("key_delete_test"));
    Metric metric = getMetric("key_delete_test");
    assertThat(metric).isNull();
  }

  /**
   * SONAR-4083
   */
  @Test
  public void should_validate_manual_metrics_with_web_service() {
    try {
      orchestrator.getServer().getAdminWsClient().create(MetricCreateQuery.create("").setName("metric with blank key").setType("INT").setDomain("domain"));
      fail();
    } catch (ConnectionException e) {
      assertThat(e.getMessage().contains("400"));
    }

    try {
      orchestrator.getServer().getAdminWsClient().create(MetricCreateQuery.create("key 1").setName("metric with space in key").setType("INT").setDomain("domain"));
      fail();
    } catch (ConnectionException e) {
      assertThat(e.getMessage().contains("400"));
    }

    try {
      orchestrator.getServer().getAdminWsClient().create(MetricCreateQuery.create("key").setName("metric without type"));
      fail();
    } catch (ConnectionException e) {
      assertThat(e.getMessage().contains("400"));
    }
  }

  /**
   * See SONAR-3873
   */
  @Test
  public void should_delete_manual_metric_also_delete_manual_measures() {
    analyzeProject();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("create-manual-metric",
      "/selenium/manual-measures/manual-metrics/create_manual_metric_and_manual_measure.html").build();
    orchestrator.executeSelenese(selenese);

    analyzeProject();

    selenese = Selenese.builder().setHtmlTestsInClasspath("delete-manual-metric",
      "/selenium/manual-measures/manual-metrics/delete_manual_metric.html").build();
    orchestrator.executeSelenese(selenese);

    analyzeProject();
  }

  private void analyzeProject() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  private void setTeamSize(int i) {
    orchestrator.getServer().getAdminWsClient().create(ManualMeasureCreateQuery.create(PROJECT_KEY, "team_size").setIntValue(i));
  }

  private void setBurnedBudget(double d) {
    orchestrator.getServer().getAdminWsClient().create(ManualMeasureCreateQuery.create(PROJECT_KEY, "burned_budget").setValue(d));
  }

  private void deleteManualMeasure(String metricKey) {
    orchestrator.getServer().getAdminWsClient().delete(ManualMeasureDeleteQuery.create(PROJECT_KEY, metricKey));
  }

  private Measure getMeasure(String metricKey) {
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, metricKey));
    return resource != null ? resource.getMeasure(metricKey) : null;
  }

  private Metric getMetric(String metricKey) {
    return orchestrator.getServer().getWsClient().find(MetricQuery.byKey(metricKey));
  }

}
