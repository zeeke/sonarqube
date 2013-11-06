/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.automated;

import com.github.kevinsawicki.http.HttpRequest;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.fail;

public class WebTest extends PerfTestCase {
  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-3.6.xml"))
    .build();


  @BeforeClass
  public static void scan_struts() throws Exception {
    FileLocation strutsHome = FileLocation.ofShared("it-sonar-performancing/struts-1.3.9/pom.xml");
    MavenBuild scan = MavenBuild.create(strutsHome.getFile());
    scan.setGoals("sonar:sonar -V");
    scan.setEnvironmentVariable("MAVEN_OPTS", "-Xmx512m");
    orchestrator.executeBuild(scan);
  }

  @Test
  public void homepage() throws Exception {
    PageStats counters = request("/");
    assertDuration(counters.durationMs, 93);
  }

  @Test
  public void quality_profiles() throws Exception {
    PageStats counters = request("/profiles");
    assertDuration(counters.durationMs, 283);
  }

  @Test
  public void issues_search() throws Exception {
    PageStats counters = request("/issues/search");
    assertDuration(counters.durationMs, 40);
  }

  @Test
  public void measures_search() throws Exception {
    PageStats counters = request("/measures");
    assertDuration(counters.durationMs, 457);
  }

  @Test
  public void all_projects() throws Exception {
    PageStats counters = request("/all_projects?qualifier=TRK");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void project_measures_search() throws Exception {
    PageStats counters = request("/measures/search?qualifiers[]=TRK");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void file_measures_search() throws Exception {
    PageStats counters = request("/measures/search?qualifiers[]=FIL");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void struts_dashboard() throws Exception {
    PageStats counters = request("/dashboard/index/org.apache.struts:struts-parent");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void struts_issues() throws Exception {
    PageStats counters = request("/issues/search?componentRoots=org.apache.struts:struts-parent");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void struts_issues_drilldown() throws Exception {
    PageStats counters = request("/drilldown/issues/org.apache.struts:struts-parent");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void struts_measures_drilldown() throws Exception {
    PageStats counters = request("/drilldown/measures/org.apache.struts:struts-parent?metric=ncloc");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void struts_cloud() throws Exception {
    PageStats counters = request("/cloud/index/org.apache.struts:struts-parent");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void struts_hotspot() throws Exception {
    PageStats counters = request("/dashboard/index/org.apache.struts:struts-parent?name=Hotspots");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void stylesheet_file() throws Exception {
    PageStats counters = request("/stylesheets/sonar.css");
    assertDuration(counters.durationMs, 66);
  }

  @Test
  public void javascript_file() throws Exception {
    PageStats counters = request("/javascripts/sonar.js");
    assertDuration(counters.durationMs, 66);
  }

  PageStats request(String path) {
    String url = orchestrator.getServer().getUrl() + path;

    // cold request
    newRequest(url).code();

    HttpRequest request = newRequest(url);
    long start = System.currentTimeMillis();
    if (request.ok()) {
      long duration = System.currentTimeMillis() - start;
      int size = request.body().length();
      PageStats counters = new PageStats(duration, size);
      System.out.printf("Page %s loaded in %d ms - size is %d bytes\n", path, counters.durationMs, counters.sizeBytes);
    }
    fail(String.format("Failed to load page: %s (%d)", url, request.code()));
    return new PageStats(Long.MAX_VALUE, Long.MAX_VALUE);
  }

  private HttpRequest newRequest(String url) {
    HttpRequest request = HttpRequest.get(url);
    request.followRedirects(false).acceptJson().acceptCharset(HttpRequest.CHARSET_UTF8);
    return request;
  }

  static class PageStats {
    long durationMs;
    long sizeBytes;

    PageStats(long durationMs, long sizeBytes) {
      this.durationMs = durationMs;
      this.sizeBytes = sizeBytes;
    }
  }
}
