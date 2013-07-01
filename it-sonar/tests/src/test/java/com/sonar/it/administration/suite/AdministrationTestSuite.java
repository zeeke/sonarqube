/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
//  BackupTest.class,
//  BulkDeletionTest.class,
//  DashboardSharingPermissionTest.class,
  PermissionsTest.class,
//  ProjectAdministrationTest.class,
//  PropertySetsTest.class,
//  SubCategoriesTest.class,
  SystemAdminPermissionTest.class,
//  UserAdministrationTest.class
})
public class AdministrationTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setServerProperty("sonar.notifications.delay", "1")
    .addPlugin(ItUtils.locateTestPlugin("crash-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("property-sets-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("subcategories-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("user-handler-plugin"))
    .build();
}
