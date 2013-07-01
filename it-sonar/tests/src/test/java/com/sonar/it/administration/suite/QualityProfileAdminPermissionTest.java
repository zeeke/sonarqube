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
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("create-user-and-profile-admin",
        "/selenium/administration/profile-admin/create-user.html",
        "/selenium/administration/profile-admin/create-profile-administrator.html",
        // Verify normal user is not allowed to do any modification
        "/selenium/administration/profile-admin/normal-user.html",
        // Verify profile admin is allowed to do modifications
        "/selenium/administration/profile-admin/profile-admin.html"

        ).build();
    orchestrator.executeSelenese(selenese);
  }
}
