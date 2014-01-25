/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import com.sonar.orchestrator.util.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.mail.internet.MimeMessage;

import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;

public class AlertNotificationsTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/com/sonar/it/profile/AlertNotificationsTest/SimpleAlertProfile.xml"))
    // 1 second
    .setServerProperty("sonar.notifications.delay", "1")
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
        "/selenium/profile/notifications/email_configuration.html",
        "/selenium/profile/notifications/create_user_with_email.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  /**
   * SONAR-2746
   */
  @Test
  public void notificationsForReviews() throws Exception {
    // Run a first analysis
    MavenBuild build = MavenBuild
      .create(ItUtils.locateProjectPom("shared/sample"))
      .setProperty("sonar.language", "java")
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    // Run a new analysis so that we get the alert
    build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.language", "java")
      .setProperty("sonar.profile.java", "SimpleAlertProfile");
    orchestrator.executeBuild(build);

    // We need to wait until all notifications will be delivered
    Thread.sleep(10000);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
    assertThat((String) message.getContent()).contains("This is a test message from Sonar");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("Alert level: Red");
    assertThat((String) message.getContent()).contains("New alert: Lines of code > 5");
    assertThat((String) message.getContent()).contains("/dashboard/index/com.sonarsource.it.samples:simple-sample");

    assertThat(emails.hasNext()).isFalse();
  }
}
