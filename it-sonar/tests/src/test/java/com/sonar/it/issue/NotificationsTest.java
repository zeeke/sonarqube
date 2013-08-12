/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue;

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
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.mail.internet.MimeMessage;
import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;

public class NotificationsTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-2.7.xml"))
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
        "/selenium/issue/notification/email_configuration.html",
        "/selenium/issue/notification/create_user_with_email.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @AfterClass
  public static void stop() {
    if (smtpServer != null) {
      smtpServer.stop();
    }
  }

  @Test
  public void notifications_for_new_issues_and_issue_changes() throws Exception {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.projectDate", "2011-12-15")
      .setProperty("sonar.profile", "sonar-way-2.7");
    orchestrator.executeBuild(build);

    // change severity
    IssueClient issueClient = ItUtils.newWsClientForAdmin(orchestrator).issueClient();
    Issues issues = issueClient.find(IssueQuery.create().componentRoots("com.sonarsource.it.samples:simple-sample"));
    Issue issue = issues.list().get(0);
    issueClient.assign(issue.key(), "tester");

    // We need to wait until all notifications will be delivered
    Thread.sleep(10000);

    Iterator<WiserMessage> emails = smtpServer.getMessages().iterator();

    MimeMessage message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<test@example.org>");
    assertThat((String) message.getContent()).contains("This is a test message from SonarQube");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("Project: Sonar :: Integration Tests :: Simple Sample");
    assertThat((String) message.getContent()).contains("3 new issues");
    assertThat((String) message.getContent()).contains("See it in SonarQube: http://localhost:9000/issues/search?componentRoots=com.sonarsource.it.samples%3Asimple-sample&createdAfter=2011-12-15T00%3A00%3A00%2B0100");

    assertThat(emails.hasNext()).isTrue();
    message = emails.next().getMimeMessage();
    assertThat(message.getHeader("To", null)).isEqualTo("<tester@example.org>");
    assertThat((String) message.getContent()).contains("sample.Sample");
    assertThat((String) message.getContent()).contains("Assignee changed to Tester");
    assertThat((String) message.getContent()).contains("See it in SonarQube: http://localhost:9000/issue/show/" + issue.key());

    assertThat(emails.hasNext()).isFalse();
  }
}
