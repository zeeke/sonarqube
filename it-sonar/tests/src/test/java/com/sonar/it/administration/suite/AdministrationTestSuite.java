/*
 * Copyright (C) 2009-2014 SonarSource SA
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
  BulkDeletionTest.class,
  ProjectAdministrationTest.class,
  PropertySetsTest.class,
  SubCategoriesTest.class,
  ProjectProvisioningTest.class
})
public class AdministrationTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setServerProperty("sonar.notifications.delay", "1")
    .addPlugin(ItUtils.locateTestPlugin("crash-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("property-sets-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("sonar-subcategories-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.javaPlugin())
    .build();
}
