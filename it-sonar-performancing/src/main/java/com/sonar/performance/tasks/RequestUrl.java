/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.github.kevinsawicki.http.HttpRequest;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.performance.Counters;

public class RequestUrl {

  public static Counters get(Orchestrator orchestrator, String path) {
    Counters counters = new Counters();

    String url = orchestrator.getServer().getUrl() + path;
    long start = System.currentTimeMillis();
    HttpRequest request = HttpRequest.get(url).followRedirects(false).acceptJson();

    if (request.ok()) {
      long end = System.currentTimeMillis();
      long size = request.body().length();
      counters.set("Time", end - start, "ms").set("Size", size, "b");
    }
    return counters;
  }
}
