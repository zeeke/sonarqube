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

  public RequestUrl(String name, String path) {
    super(name);
    this.path = path;
  }

  @Override
  public int replay() {
    return 3;
  }

  public void execute(Orchestrator orchestrator, Counters counters) {
    String url = orchestrator.getServer().getUrl() + path;
    long start = System.currentTimeMillis();
    HttpRequest request = HttpRequest.get(url).followRedirects(false).acceptJson();

    if (request.ok()) {
      long end = System.currentTimeMillis();
      long size = request.body().length();
      counters
        .set("Time (ms)", end - start)
        .set("Size (bytes)", size);
    }
  }
}
