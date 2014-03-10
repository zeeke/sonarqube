/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.qualitygate;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

public class QualityGateTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();

  @Test
  public void should_not_compute_alert_status_if_no_quality_gate_exist() {
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchResourceWithAlertStatus()).isNull();
  }

  @Test
  public void should_compute_alert_status_ok_with_default_empty_quality_gate() {
    QualityGate empty = qgClient().create("Empty");
    qgClient().setDefault(empty.id());

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("OK");

    qgClient().unsetDefault();
    qgClient().destroy(empty.id());
  }

  @Test
  public void should_compute_alert_status_ok_when_threshold_not_met() {
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
  public void should_compute_alert_status_warning_with_default_quality_gate() {
    QualityGate simple = qgClient().create("SimpleWithLowThreshold");
    qgClient().setDefault(simple.id());
    qgClient().createCondition(NewCondition.create(simple.id()).metricKey("ncloc").operator("GT").warningThreshold("10"));

    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("qualitygate/xoo-sample"));
    orchestrator.executeBuild(build);

    assertThat(fetchAlertStatus().getData()).isEqualTo("WARN");

    qgClient().unsetDefault();
    qgClient().destroy(simple.id());
  }

  private Measure fetchAlertStatus() {
    return fetchResourceWithAlertStatus().getMeasure("alert_status");
  }

  private Resource fetchResourceWithAlertStatus() {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "alert_status"));
  }

  private static QualityGateClient qgClient() {
    return orchestrator.getServer().adminWsClient().qualityGateClient();
  }
}
