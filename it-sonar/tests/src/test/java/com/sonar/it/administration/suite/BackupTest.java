/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import java.io.IOException;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;

public class BackupTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  /**
   * See SONAR-3930
   */
  @Test
  public void should_backup_settings_xml_contains_only_database_properties() {
    String settings = getBackupSettings();

    assertThat(settings)
      .doesNotContain("sonar.web.deployDir")
      .doesNotContain("java.home")
      .doesNotContain("PWD");

    assertThat(settings)
      .contains("sonar.profile.java")
      .contains("sonar.core.version")
      .contains("sonar.core.startTime");
  }

  @Test
  public void should_backup_settings_xml_contains_newly_added_property() {
    String key = "sonar.it.backup.settings.key";
    String settings = getBackupSettings();
    assertThat(settings).doesNotContain(key);

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery(key, "test"));
    settings = getBackupSettings();
    assertThat(settings).contains(key);
  }

  /**
   * See SONAR-1352
   */
  @Test
  public void should_backup_settings_xml_contains_alert() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-backup",
      "/selenium/administration/project-backup/add-alerts-to-quality-profile.html").build();
    orchestrator.executeSelenese(selenese);

    String settings = getBackupSettings();

    assertThat(settings)
      .contains("<operator><![CDATA[<]]></operator>")
      .contains("<value-error><![CDATA[1]]></value-error>")
      .contains("<value-warning><![CDATA[2]]></value-warning>")
      .contains("<![CDATA[complexity]]>");

    assertThat(settings)
      .contains("<operator><![CDATA[>]]></operator>")
      .contains("<value-error><![CDATA[3]]></value-error>")
      .contains("<value-warning><![CDATA[4]]></value-warning>")
      .contains("<period><![CDATA[1]]></period>")
      .contains("<![CDATA[class_complexity]]>");

    assertThat(settings)
      .contains("<operator><![CDATA[=]]></operator>")
      .contains("<value-error><![CDATA[5]]></value-error>")
      .contains("<value-warning><![CDATA[6]]></value-warning>")
      .contains("<period><![CDATA[2]]></period>")
      .contains("<![CDATA[file_complexity]]>");

    assertThat(settings)
      .contains("<operator><![CDATA[!=]]></operator>")
      .contains("<value-error><![CDATA[7]]></value-error>")
      .contains("<value-warning><![CDATA[8]]></value-warning>")
      .contains("<period><![CDATA[3]]></period>")
      .contains("<![CDATA[function_complexity]]>");
  }

  private String getBackupSettings() {
    // this is not a web service, so we can't use sonar-ws-client
    DefaultHttpClient client = new DefaultHttpClient();
    String result = null;
    try {
      String url = orchestrator.getServer().getUrl();
      HttpHost targetHost = new HttpHost("localhost", new URL(url).getPort(), "http");

      HttpParams params = client.getParams();
      HttpConnectionParams.setConnectionTimeout(params, 60000);
      HttpConnectionParams.setSoTimeout(params, 120000);

      client.getCredentialsProvider().setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), new UsernamePasswordCredentials("admin", "admin"));

      BasicHttpContext localContext = new BasicHttpContext();
      BasicScheme basicAuth = new BasicScheme();
      localContext.setAttribute("preemptive-auth", basicAuth);
      client.addRequestInterceptor(new PreemptiveAuth(), 0);

      HttpGet httpget = new HttpGet(url + "/backup/export");
      HttpResponse response = client.execute(httpget, localContext);
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        long len = entity.getContentLength();
        if (len != -1) {
          result = EntityUtils.toString(entity);
        } else {
          throw new RuntimeException("The content of the export is null");
        }
      }

    } catch (IOException e) {
      throw new IllegalStateException("Fail to export settings", e);
    } finally {
      client.getConnectionManager().shutdown();
      return result;
    }

  }

  static final class PreemptiveAuth implements HttpRequestInterceptor {
    public void process(
      final HttpRequest request,
      final HttpContext context) throws HttpException {

      AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

      if (authState.getAuthScheme() == null) {
        AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
        HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        if (authScheme != null) {
          Credentials creds = credsProvider.getCredentials(
            new AuthScope(targetHost.getHostName(), targetHost.getPort()));
          if (creds == null) {
            throw new HttpException("No credentials for preemptive authentication");
          }
          authState.setAuthScheme(authScheme);
          authState.setCredentials(creds);
        }
      }
    }
  }

}
