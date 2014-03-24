/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.server;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ServerTest {

  Orchestrator orchestrator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void load_embedded_libraries() throws IOException {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("embedded-library-plugin"))
      .build();
    orchestrator.start();

    // Should raise an exception because server is not started
    Server server = orchestrator.getServer().getWsClient().find(new ServerQuery());
    assertThat(server.getStatus()).isEqualTo(Server.Status.UP);

    assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()))
      .contains("Embedded dependency from server extension");
  }

  @Test
  public void test_ruby_web_service_extension() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("ruby-ws-plugin"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server-ruby-ws",
      "/selenium/server/ruby-extensions/ruby-ws.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2727
   */
  @Test
  public void display_warnings_when_using_h2() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    if (builder.getOrchestratorConfiguration().getString("sonar.jdbc.dialect").equals("h2")) {
      orchestrator = builder.build();
      orchestrator.start();

      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("derby-warnings",
        "/selenium/server/derby-warning.html").build();
      orchestrator.executeSelenese(selenese);
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2840
   */
  @Test
  public void hide_jdbc_settings_to_non_admin() {
    orchestrator = Orchestrator.createEnv();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("jdbc-settings",
      "/selenium/server/hide-jdbc-settings.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_settings() {
    URL secretKeyUrl = getClass().getResource("/com/sonar/it/server/ServerTest/sonar-secret.txt");
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("settings-plugin"))
      .addPlugin(ItUtils.locateTestPlugin("license-plugin"))
      .setServerProperty("sonar.secretKeyPath", secretKeyUrl.getFile())
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings",
      "/selenium/server/settings/general-settings.html",

      // SONAR-2869 the annotation @Properties can be used on extensions and not only on plugin entry points
      "/selenium/server/settings/hidden-extension-property.html",
      "/selenium/server/settings/global-extension-property.html",

      // SONAR-3344 - licenses
      "/selenium/server/settings/ignore-corrupted-license.html",
      "/selenium/server/settings/display-license.html",
      "/selenium/server/settings/display-untyped-license.html",

      // SONAR-2084 - encryption
      "/selenium/server/settings/generate-secret-key.html",
      "/selenium/server/settings/encrypt-text.html",

      // SONAR-1378 - property types
      "/selenium/server/settings/validate-property-type.html",

      // SONAR-3127 - hide passwords
      "/selenium/server/settings/hide-passwords.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void property_relocation() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("property-relocation-plugin"))
      .addPlugin(ItUtils.xooPlugin())
      .setServerProperty("sonar.deprecatedKey", "true")
      .build();
    orchestrator.start();

    SonarRunner withDeprecatedKey = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("sonar.deprecatedKey", "true");
    SonarRunner withNewKey = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperty("sonar.newKey", "true");
    // should not fail
    orchestrator.executeBuilds(withDeprecatedKey, withNewKey);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("property_relocation",
      "/selenium/server/settings/property_relocation.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3105
   */
  @Test
  public void test_projects_web_service() throws IOException {
    orchestrator = Orchestrator.createEnv();
    orchestrator.start();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    String url = orchestrator.getServer().getUrl() + "/api/projects?key=com.sonarsource.it.samples:simple-sample&versions=true";
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(url);
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      String content = IOUtils.toString(response.getEntity().getContent());
      assertThat(content).doesNotContain("error");
      assertThat(content).contains("simple-sample");
      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  /**
   * SONAR-3320
   */
  @Test
  public void global_property_change_extension_point() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("global-property-change-plugin"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server-global-property-change",
      // the plugin listens to changes of global properties and stores changes in the property globalPropertyChange.received
      "/selenium/server/global-property-change/extension-is-subscribed-to-changes.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3516
   */
  @Test
  public void check_minimal_sonar_version_at_startup() throws Exception {
    try {
      orchestrator = Orchestrator.builderEnv()
        .addPlugin(FileLocation.of(new File(ServerTest.class.getResource("/com/sonar/it/server/ServerTest/incompatible-plugin-1.0.jar").toURI())))
        .build();
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs())).contains("Plugin incompatibleplugin needs a more recent version of SonarQube")
        .contains("At least 5.9 is expected");
    }
  }

  /**
   * SONAR-3962
   */
  @Test
  public void not_fail_with_url_ending_by_jsp() {
    orchestrator = Orchestrator.builderEnv().build();
    orchestrator.start();

    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/sample"))
      .setProperty("sonar.projectKey", "myproject.jsp"));
    // Access dashboard
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("url_ending_by_jsp",
      "/selenium/server/url_ending_by_jsp.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void support_install_dir_with_whitespaces() throws Exception {
    String dirName = "target/has space";
    FileUtils.deleteDirectory(new File(dirName));
    orchestrator = Orchestrator.builderEnv().setOrchestratorProperty("orchestrator.workspaceDir", dirName).build();
    orchestrator.start();

    Server.Status status = orchestrator.getServer().getAdminWsClient().find(new ServerQuery()).getStatus();
    assertThat(status).isEqualTo(Server.Status.UP);
  }

  // SONAR-4404
  @Test
  public void should_get_settings_default_value() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("server-plugin"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings-default-value",
      "/selenium/server/settings-default-value.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4748
  @Test
  public void should_create_in_temp_folder() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("server-plugin"))
      .setServerProperty("sonar.createTempFiles", "true")
      .build();
    orchestrator.start();

    File tempDir = new File(orchestrator.getServer().getHome(), "temp/tmp");

    String logs = FileUtils.readFileToString(orchestrator.getServer().getLogs());
    assertThat(logs).contains("Creating temp directory: " + tempDir.getAbsolutePath() + File.separator + "sonar-it");
    assertThat(logs).contains("Creating temp file: " + tempDir.getAbsolutePath() + File.separator + "sonar-it");

    // Verify temp folder is created
    assertThat(new File(tempDir, "sonar-it")).isDirectory().exists();

    orchestrator.stop();

    // Verify temp folder is deleted after shutdown
    assertThat(new File(tempDir, "sonar-it")).doesNotExist();
  }

  /**
   * SONAR-4843
   */
  @Test
  @Ignore("Wait for next goldenisation")
  public void restart_forbidden_if_not_dev_mode() throws Exception {
    orchestrator = Orchestrator.builderEnv().build();
    orchestrator.start();
    try {
//      orchestrator.getServer().adminWsClient().systemClient().restart();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("403");
    }
  }

  /**
   * SONAR-4843
   */
  @Test
  @Ignore("Wait for next goldenisation")
  public void restart_on_dev_mode() throws Exception {
    orchestrator = Orchestrator.builderEnv().setServerProperty("sonar.dev", "true").build();
    orchestrator.start();

//    orchestrator.getServer().adminWsClient().systemClient().restart();
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()))
      .contains("Restart server")
      .contains("Server restarted");
  }
}
