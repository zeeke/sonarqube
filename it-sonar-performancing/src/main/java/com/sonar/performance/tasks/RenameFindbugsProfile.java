/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance.tasks;

import com.github.kevinsawicki.http.HttpRequest;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;

public class RenameFindbugsProfile {

  /**
   * Just because we can't execute a scan with -Dsonar.profile='Sonar Way with Findbugs'. Orchestrator
   * does not support whitespaces.
   */
  public static void execute(Orchestrator orchestrator) {
    HttpRequest request = HttpRequest.get(orchestrator.getServer().getUrl() + "/profiles").basic("admin", "admin");
    if (!request.ok()) {
      throw new IllegalStateException("Fail to request Quality profiles: " + request.message());
    }

    String html = request.body();
    if (!html.contains("findbugs-profile")) {
      if (!html.contains("Sonar way with Findbugs")) {
        throw new IllegalStateException("Please create the Quality profile 'Sonar way with Findbugs' before starting performance profiling");
      }
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("rename-findbugs-profile", "/selenium/rename_findbugs_profile.html").build();
      orchestrator.executeSelenese(selenese);
    }
  }
}
