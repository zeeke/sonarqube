/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.user.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

public class UserAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = UserTestSuite.ORCHESTRATOR;

  /**
   * SONAR-4827
   */
  @Test
  public void manage_users() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-users",
      "/selenium/user/manage-users/min-login-size.html",
      "/selenium/user/manage-users/accept-login-with-uppercase.html",
      "/selenium/user/manage-users/accept-login-with-whitespace.html",
      "/selenium/user/manage-users/can-not-delete-myself.html",
      "/selenium/user/manage-users/change-username-without-changing-password.html",
      "/selenium/user/manage-users/confirm-password-when-creating-user.html",
      "/selenium/user/manage-users/create-and-delete-users.html",
      "/selenium/user/manage-users/my-profile-display-basic-data.html",
      "/selenium/user/manage-users/authenticate-new-user.html",
      // SONAR-3258
      "/selenium/user/manage-users/delete-and-reactivate-user.html",
      // SONAR-3258
      "/selenium/user/manage-users/delete-user-and-check-not-available-in-selections.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4827
   */
  @Test
  @Ignore("Ignored because of too many F/P on jenkins")
  public void manage_groups() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-groups",
      "/selenium/user/manage-groups/admin-has-default-groups.html",
      "/selenium/user/manage-groups/affect-new-user-to-default-group.html",
      "/selenium/user/manage-groups/affect-user-to-group.html",
      "/selenium/user/manage-groups/create-and-delete-groups.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

}
