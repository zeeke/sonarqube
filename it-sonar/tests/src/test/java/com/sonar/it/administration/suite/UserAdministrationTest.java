/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;

public class UserAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @Test
  public void manage_users() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-users",
      "/selenium/administration/manage-users/min-login-size.html",
      "/selenium/administration/manage-users/accept-login-with-uppercase.html",
      "/selenium/administration/manage-users/accept-login-with-whitespace.html",
      "/selenium/administration/manage-users/can-not-delete-myself.html",
      "/selenium/administration/manage-users/change-username-without-changing-password.html",
      "/selenium/administration/manage-users/confirm-password-when-creating-user.html",
      "/selenium/administration/manage-users/create-and-delete-users.html",
      "/selenium/administration/manage-users/my-profile-display-basic-data.html",
      "/selenium/administration/manage-users/authenticate-new-user.html",
      // SONAR-3258
      "/selenium/administration/manage-users/delete-and-reactivate-user.html",
      // SONAR-3258
      "/selenium/administration/manage-users/delete-user-and-check-not-available-in-selections.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void manage_groups() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage-groups",
      "/selenium/administration/manage-groups/add-user-to-group.html",
      "/selenium/administration/manage-groups/admin-has-default-groups.html",
      "/selenium/administration/manage-groups/affect-new-user-to-default-group.html",
      "/selenium/administration/manage-groups/affect-user-to-group.html",
      "/selenium/administration/manage-groups/create-and-delete-groups.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

}
