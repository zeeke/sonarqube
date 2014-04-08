/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyQuery;

import static org.fest.assertions.Assertions.assertThat;

public class SubCategoriesTest {
  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  /**
   * SONAR-3159
   */
  @Test
  public void should_support_global_subcategories() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("subcategories",
        "/selenium/administration/subcategories/global-subcategories.html",
        // SONAR-4495
        "/selenium/administration/subcategories/global-subcategories-no-default.html"
        ).build();
    orchestrator.executeSelenese(selenese);
    assertThat(getProperty("prop3", null)).isEqualTo("myValue");
  }

  /**
   * SONAR-3159
   */
  @Test
  public void should_support_project_subcategories() {
    orchestrator.executeBuild(SonarRunner.create(ItUtils.locateProjectDir("shared/sample")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("subcategories",
        "/selenium/administration/subcategories/project-subcategories.html",
        // SONAR-4495
        "/selenium/administration/subcategories/project-subcategories-no-default.html"
        ).build();
    orchestrator.executeSelenese(selenese);
    assertThat(getProperty("prop3", "sample")).isEqualTo("myValue2");
  }

  static String getProperty(String key, String resourceKeyOrId) {
    return orchestrator.getServer().getAdminWsClient().find(new PropertyQuery().setKey(key).setResourceKeyOrId(resourceKeyOrId)).getValue();
  }
}
