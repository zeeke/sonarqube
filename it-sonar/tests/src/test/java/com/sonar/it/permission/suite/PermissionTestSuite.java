/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.permission.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  PermissionsTest.class, IssuePermissionTest.class, ProjectPermissionsTest.class,
  DashboardSharingPermissionTest.class, DryRunScanPermissionTest.class, ScanPermissionTest.class, SystemAdminPermissionTest.class,
  QualityProfileAdminPermissionTest.class, SourceCodePermissionTest.class
})
public class PermissionTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();
}
