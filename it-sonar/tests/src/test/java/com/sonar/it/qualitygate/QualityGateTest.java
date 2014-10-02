/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.qualitygate;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class QualityGateTest {

  private static final String PROJECT_KEY = "sample";

  private long provisionnedProjectId = -1L;

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();

  @Before
  public void cleanUp() throws Exception {
    orchestrator.resetData();
    provisionnedProjectId = Long.parseLong(orchestrator.getServer().adminWsClient().projectClient().create(NewProject.create().key(PROJECT_KEY).name("Sample")).id());
  }

  @Test
  public void not_compute_alert_status_if_no_quality_gate_exist() {
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchResourceWithAlertStatus()).isNull();
  }

  @Test
  public void compute_alert_status_ok_with_default_empty_quality_gate() {
    QualityGate empty = qgClient().create("Empty");
    qgClient().setDefault(empty.id());

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("OK");

    qgClient().unsetDefault();
    qgClient().destroy(empty.id());
  }

  @Test
  public void compute_alert_status_ok_when_threshold_not_met() {
    QualityGate simple = qgClient().create("SimpleWithHighThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").warningThreshold("20"));

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("OK");

    qgClient().unsetDefault();
    qgClient().destroy(simple.id());
  }

  @Test
  public void compute_alert_status_warning_with_default_quality_gate() {
    QualityGate simple = qgClient().create("SimpleWithLowThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("WARN");

    qgClient().unsetDefault();
    qgClient().destroy(simple.id());
  }

  @Test
  public void compute_alert_status_error_with_default_quality_gate() {
    QualityGate simple = qgClient().create("SimpleWithLowThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").errorThreshold("10"));

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("ERROR");

    qgClient().unsetDefault();
    qgClient().destroy(simple.id());
  }

  @Test
  public void use_local_settings_instead_of_default_qgate() {
    QualityGate alert = qgClient().create("AlertWithLowThreshold");
    qgClient().createCondition(NewCondition.create(alert.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));
    QualityGate error = qgClient().create("ErrorWithLowThreshold");
    qgClient().createCondition(NewCondition.create(error.id()).metricKey("ncloc").operator("GT").errorThreshold("10"));

    qgClient().setDefault(alert.id());

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample")).setProperty("sonar.qualitygate", "ErrorWithLowThreshold");
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("ERROR");

    qgClient().unsetDefault();
    qgClient().destroy(alert.id());
    qgClient().destroy(error.id());
  }

  @Test
  public void use_server_settings_instead_of_default_qgate() {
    QualityGate alert = qgClient().create("AlertWithLowThreshold");
    qgClient().createCondition(NewCondition.create(alert.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));
    QualityGate error = qgClient().create("ErrorWithLowThreshold");
    qgClient().createCondition(NewCondition.create(error.id()).metricKey("ncloc").operator("GT").errorThreshold("10"));

    qgClient().setDefault(alert.id());
    qgClient().selectProject(error.id(), provisionnedProjectId);

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("ERROR");

    qgClient().unsetDefault();
    qgClient().destroy(alert.id());
    qgClient().destroy(error.id());
  }

  @Test
  public void raise_alerts_on_multiple_metric_types() {
    QualityGate allTypes = qgClient().create("AllMetricTypes");
    qgClient().createCondition(NewCondition.create(allTypes.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));
    qgClient().createCondition(NewCondition.create(allTypes.id()).metricKey("file_complexity").operator("GT").warningThreshold("7.5"));
    qgClient().createCondition(NewCondition.create(allTypes.id()).metricKey("duplicated_lines_density").operator("GT").warningThreshold("20"));
    qgClient().setDefault(allTypes.id());

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"))
      .setProperty("sonar.cpd.xoo.minimumLines", "2")
      .setProperty("sonar.cpd.xoo.minimumTokens", "5");
    orchestrator.executeBuild(build);

    Measure alertStatus = fetchAlertStatus();
    assertThat(alertStatus.getData()).isEqualTo("WARN");
    assertThat(alertStatus.getAlertText())
      .contains("Lines of code > 10")
      .contains("Complexity /file > 7.5")
      .contains("Duplicated lines (%) > 20");

    qgClient().unsetDefault();
    qgClient().destroy(allTypes.id());
  }

  @Test
  public void compute_alert_status_on_metric_variation() {
    QualityGate simple = qgClient().create("SimpleWithDifferential");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").period(1).operator("EQ").warningThreshold("0"));

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);
    assertThat(fetchAlertStatus().getData()).isEqualTo("OK");

    orchestrator.executeBuild(build);
    assertThat(fetchAlertStatus().getData()).isEqualTo("WARN");

    qgClient().unsetDefault();
    qgClient().destroy(simple.id());
  }

  private Measure fetchAlertStatus() {
    return fetchResourceWithAlertStatus().getMeasure("alert_status");
  }

  private Resource fetchResourceWithAlertStatus() {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "alert_status").setIncludeAlerts(true));
  }

  private static QualityGateClient qgClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }
}
