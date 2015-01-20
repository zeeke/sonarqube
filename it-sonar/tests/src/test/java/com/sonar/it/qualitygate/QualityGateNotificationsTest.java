/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.qualitygate;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import com.sonar.orchestrator.util.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.qualitygate.NewCondition;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.mail.internet.MimeMessage;

import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;

public class QualityGateNotificationsTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    // 1 second
    .setServerProperty("sonar.notifications.delay", "1")
    .addPlugin(ItUtils.javaPlugin())
    .build();

  private static Wiser smtpServer;

  @BeforeClass
  public static void before() throws Exception {
    smtpServer = new Wiser(NetworkUtils.getNextAvailablePort());
    smtpServer.start();
    System.out.println("SMTP Server port: " + smtpServer.getServer().getPort());

    // Configure Sonar
    Sonar wsClient = orchestrator.getServer().getAdminWsClient();
    wsClient.update(new PropertyUpdateQuery("email.smtp_host.secured", "localhost"));
    wsClient.update(new PropertyUpdateQuery("email.smtp_port.secured", Integer.toString(smtpServer.getServer().getPort())));

    // 1. Check that SMTP server was turned on and able to send test email
    // 2. Create user, which will receive notifications for new violations
    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("notifications",
        "/selenium/qualitygate/notifications/email_configuration.html",
        "/selenium/qualitygate/notifications/create_user_with_email.html").build();
    orchestrator.executeSelenese(selenese);

    // Create quality gate and condition
    QualityGateClient qgClient = orchestrator.getServer().adminWsClient().qualityGateClient();
    QualityGate qGate = qgClient.create("SimpleQualityGate");
    qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("ncloc").operator("GT").warningThreshold("2").errorThreshold("5"));
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  /**
   * SONAR-2746
   * SONAR-4366
   */
  @Test
  public void should_send_notifications_on_quality_gate_status_change() throws Exception {
    // Run a first analysis
    MavenBuild build = MavenBuild
      .create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    // Run a new analysis so that we get the alert
    build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.qualitygate", "SimpleQualityGate");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:simple-sample", "alert_status"));
    assertThat(project.getMeasure("alert_status").getData()).isEqualTo("ERROR");

    // We need to wait until all notifications are delivered
    Thread.sleep(10000);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
    assertThat((String) message.getContent()).contains("This is a test message from Sonar");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("Quality gate status: Red");
    assertThat((String) message.getContent()).contains("New quality gate threshold: Lines of code > 5");
    assertThat((String) message.getContent()).contains("/dashboard/index/com.sonarsource.it.samples:simple-sample");

    assertThat(emails.hasNext()).isFalse();
  }
}
