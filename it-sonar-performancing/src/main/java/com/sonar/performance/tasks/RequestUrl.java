/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.github.kevinsawicki.http.HttpRequest;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.Counters;
import com.sonar.performance.PerformanceTask;

public class RequestUrl extends PerformanceTask {

  private final String path;
  private boolean post;
  private boolean admin;

  public RequestUrl(String name, String path) {
    super(name);
    this.path = path;
    this.post = false;
    this.admin = false;
  }

  public RequestUrl post() {
    this.post = true;
    return this;
  }

  public RequestUrl admin() {
    this.admin = true;
    return this;
  }

  @Override
  public int replay() {
    return 3;
  }

  public void execute(Orchestrator orchestrator, Counters counters) {
    String url = orchestrator.getServer().getUrl() + path;
    long start = System.currentTimeMillis();
    HttpRequest request = (post ? HttpRequest.post(url) : HttpRequest.get(url));
    request.followRedirects(false).acceptJson().acceptCharset(HttpRequest.CHARSET_UTF8);
    if (admin) {
      request.basic("admin", "admin");
    }
    if (request.ok()) {
      long end = System.currentTimeMillis();
      long size = request.body().length();
      counters
        .set("Time (ms)", end - start)
        .set("Size (bytes)", size);
    }
  }
}
