/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONValue;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ServerAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.createEnv();

  @Test
  public void getSonarVersion() {
    assertThat(orchestrator.getServer().getWsClient().find(new ServerQuery()).getVersion(), startsWith("3."));
  }

  @Test
  public void getServerStatus() {
    assertThat(orchestrator.getServer().getWsClient().find(new ServerQuery()).getStatus(), is(Server.Status.UP));
  }

  @Test
  public void generateServerId() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server_id",
        "/selenium/server_id/missing_ip.html",
        // SONAR-4102
        "/selenium/server_id/organisation_must_not_accept_special_chars.html",
        "/selenium/server_id/valid_id.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void displaySystemInfo() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server-administration",
        "/selenium/server/server-administration/system_info.html",
        "/selenium/server/server-administration/general-settings.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void manageUsers() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("users",
        "/selenium/server/server-administration/users/min-login-size.html",
        "/selenium/server/server-administration/users/accept-login-with-uppercase.html",
        "/selenium/server/server-administration/users/accept-login-with-whitespace.html",
        "/selenium/server/server-administration/users/add-user-to-group.html",
        "/selenium/server/server-administration/users/admin-has-default-groups.html",
        "/selenium/server/server-administration/users/affect-new-user-to-default-group.html",
        "/selenium/server/server-administration/users/affect-user-to-group.html",
        "/selenium/server/server-administration/users/can-not-delete-myself.html",
        "/selenium/server/server-administration/users/change-username-without-changing-password.html",
        "/selenium/server/server-administration/users/confirm-password-when-creating-user.html",
        "/selenium/server/server-administration/users/create-and-delete-groups.html",
        "/selenium/server/server-administration/users/create-and-delete-users.html",
        "/selenium/server/server-administration/users/my-profile-display-basic-data.html",
        "/selenium/server/server-administration/users/authenticate-new-user.html",
        "/selenium/server/server-administration/users/delete-and-reactivate-user.html", // SONAR-3258
        "/selenium/server/server-administration/users/delete-user-and-check-not-available-in-selections.html" // SONAR-3258
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void configureRoles() {
    MavenBuild build = MavenBuild.builder()
        .setPom(ItUtils.locateProjectPom("shared/sample"))
        .addSonarGoal()
        .withDynamicAnalysis(false)
        .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("roles",
        "/selenium/server/server-administration/roles/display-roles.html",
        "/selenium/server/server-administration/roles/grant-default-project-groups.html",
        "/selenium/server/server-administration/roles/grant-default-project-users.html",
        "/selenium/server/server-administration/roles/grant-project-users-from-global-administration.html",
        "/selenium/server/server-administration/roles/grant-project-groups-from-global-administration.html",

        // SONAR-3383
        "/selenium/server/server-administration/roles/search-projects.html"

        ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3147
   */
  @Test
  public void testWidgetsWebService() throws IOException {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/api/widgets");
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode(), is(200));
      String json = IOUtils.toString(response.getEntity().getContent());
      List widgets = (List) JSONValue.parse(json);
      assertThat(widgets.size(), greaterThan(10));

      // quick test of the first widget
      assertNotNull(((Map) widgets.get(0)).get("title"));

      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }
}
