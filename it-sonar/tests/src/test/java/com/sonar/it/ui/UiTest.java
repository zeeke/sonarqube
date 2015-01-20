/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class UiTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("static-files-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("ruby-api-tester-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("required-measures-widgets-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("ruby-rails-app-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("page-decoration-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("resource-configuration-extension-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.javaPlugin())
    .build();

  @After
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void test_static_files() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-static-files",
      "/selenium/ui/static-files.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3555
   */
  @Test
  public void content_type_of_static_files_is_set() throws Exception {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/static/staticfilesplugin/cute.jpg");
      HttpResponse response = httpclient.execute(get);
      assertThat(response.getLastHeader("Content-Type").getValue()).isEqualTo("image/jpeg");

      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  @Test
  public void test_footer() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-footer",
      "/selenium/ui/footer.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-2376
   */
  @Test
  public void test_page_decoration() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-page-decoration",
      "/selenium/ui/page-decoration.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void http_response_should_be_gzipped() throws IOException {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl());
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(response.getLastHeader("Content-Encoding")).isNull();
      EntityUtils.consume(response.getEntity());

      get = new HttpGet(orchestrator.getServer().getUrl());
      get.addHeader("Accept-Encoding", "gzip, deflate");
      response = httpclient.execute(get);
      assertThat(response.getLastHeader("Content-Encoding").getValue()).isEqualTo("gzip");
      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  @Test
  public void test_ruby_extensions() {
    scanSample();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-ruby-extensions",
      "/selenium/ui/ruby-extensions/ruby-api-tester.html",
      "/selenium/ui/ruby-extensions/ruby-rails-app.html",
      "/selenium/ui/ruby-extensions/ruby-rails-app-advanced.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_reliability_of_web_services() throws InterruptedException {
    scanSample();

    ExecutorService executor = Executors.newFixedThreadPool(5);
    for (int i = 0; i < 100; i++) {
      Runnable worker = new WsClientWorker();
      executor.execute(worker);
    }
    executor.shutdown();
    executor.awaitTermination(600, TimeUnit.SECONDS);
  }

  /**
   * SONAR-3323
   */
  @Test
  public void should_display_widgets_according_to_required_measures() {
    scanSample();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-views",
      "/selenium/ui/views/should-display-widgets-according-to-required-measures.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4173
   */
  @Test
  public void test_resource_configuration_extension() {
    scanSample();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("resource-configuration-extension",
      "/selenium/ui/resource-configuration-extension/resource-configuration-extension.html").build();
    orchestrator.executeSelenese(selenese);
  }

  private static class WsClientWorker implements Runnable {
    public void run() {
      System.out.print(".");
      Sonar client = orchestrator.getServer().getWsClient();
      ResourceQuery query = ResourceQuery.createForMetrics("com.sonarsource.it.samples:simple-sample", "lines", "files");
      Resource resource = client.find(query);
      assertThat(resource).isNotNull();
      assertThat(resource.getMeasures()).hasSize(2);
      System.out.print("|");
    }
  }

  private void scanSample() {
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false"));
  }

  private void scanXooSample() {
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(scan);
  }

}
