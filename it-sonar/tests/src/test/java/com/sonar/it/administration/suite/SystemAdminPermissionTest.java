/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;

public class SystemAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  /**
   * SONAR-4398
   */
  @Test
  public void should_change_ownership_of_shared_measure_filter() throws Exception {

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters userCreationParameters = UserParameters.create().login("user").name("user").password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);

    seleniumSuite("change-measure-filter-ownership",
      "/selenium/administration/system-admin-permission/change-own-measure-filter-owner.html",
      "/selenium/administration/system-admin-permission/change-other-measure-filter-owner.html",
      "/selenium/administration/system-admin-permission/change-system-measure-filter-owner.html");
  }

  /**
   * SONAR-4399
   */
  @Test
  public void should_change_ownership_of_shared_issue_filter() throws Exception {

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters userCreationParameters = UserParameters.create().login("user").name("user").password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);

    seleniumSuite("change-issue-filter-ownership",
      "/selenium/administration/system-admin-permission/change-own-issue-filter-owner.html",
      "/selenium/administration/system-admin-permission/change-other-issue-filter-owner.html");
  }

  private void seleniumSuite(String suiteName, String... tests) {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(suiteName, tests).build();
    orchestrator.executeSelenese(selenese);
  }
}
