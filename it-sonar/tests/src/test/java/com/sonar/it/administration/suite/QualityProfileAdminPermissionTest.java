/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

/**
 * SONAR-4210
 */
public class QualityProfileAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/sample")));
  }

  @Test
  public void create_user_and_profile_admin() {

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    UserParameters simpleUser = UserParameters.create().login("not_profileadm").name("Not a profile admin")
      .password("userpwd").passwordConfirmation("userpwd");
    client.userClient().create(simpleUser);
    UserParameters profileAdmin = UserParameters.create().login("profileadm").name("Profile Admin")
      .password("papwd").passwordConfirmation("papwd");
    client.userClient().create(profileAdmin);
    PermissionParameters profileAdministration = PermissionParameters.create().user("profileadm").permission("profileadmin");
    client.permissionClient().addPermission(profileAdministration);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("administrate-profiles",
        // Verify normal user is not allowed to do any modification
        "/selenium/administration/profile-admin/normal-user.html",
        // Verify profile admin is allowed to do modifications
        "/selenium/administration/profile-admin/profile-admin.html"
        ).build();
    orchestrator.executeSelenese(selenese);
  }
}
