/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.user;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.After;
import org.junit.Test;

public class AuthenticationTest {
  // restarted for each test
  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void test_authentication() {
    orchestrator = Orchestrator.builderEnv().build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("authentication",
      "/selenium/user/authentication/login_cancel.html",
      "/selenium/user/authentication/login_successful.html",
      "/selenium/user/authentication/login_wrong_password.html",
      "/selenium/user/authentication/redirect_to_original_url_after_direct_login.html", // SONAR-2132
      "/selenium/user/authentication/redirect_to_original_url_after_indirect_login.html" // SONAR-2009
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3473
   */
  @Test
  public void force_authentication() {
    orchestrator = Orchestrator.builderEnv().setServerProperty("sonar.forceAuthentication", "true").build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("force_authentication",
      "/selenium/user/authentication/force-authentication.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }
}
