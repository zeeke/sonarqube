/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.cartography;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class ResourcesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.xooPlugin())
      .build();

  @After
  public void cleanup() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4235
   */
  @Test
  public void test_project_creation_date() {
    long before = new Date().getTime();
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample")));
    long after = new Date().getTime();
    Resource xooSample = orchestrator.getServer().getWsClient().find(new ResourceQuery().setResourceKeyOrId("sample"));
    assertThat(xooSample.getCreationDate().getTime()).isGreaterThan(before).isLessThan(after);
  }
}
