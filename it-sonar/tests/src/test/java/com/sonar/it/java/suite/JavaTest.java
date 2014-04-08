/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.java.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTest {

  @ClassRule
  public static Orchestrator orchestrator = JavaTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void shouldSupportJapaneseCharset() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("java/japanese-charset"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("java-japanese-charset",
      "/selenium/java-japanese-charset/japanese_duplications.html",
      "/selenium/java-japanese-charset/japanese_sources.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-3726 & SONAR-3823
  @Test
  public void shouldNotActivateJavaRelatedExtensionsIfNotJavaProject() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("java/non-java-project")).setGoals("clean", "sonar:sonar");
    String logs = orchestrator.executeBuild(build).getLogs();

    assertThat(logs).doesNotContain("Cobertura");
    assertThat(logs).doesNotContain("Checkstyle");
  }

  // SONAR-3893 & SONAR-4247
  @Test
  public void shouldHighlightJavaSourceCode() throws Exception {

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample-with-tests"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("java-syntax-highlighting",
      "/selenium/java/syntax-highlighting.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4249 & SONAR-4250
  @Test
  public void shouldHighlightJavaSymbolsUsage() throws Exception {

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample-with-tests"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("highlight-symbol-usages",
      "/selenium/java/symbol-usages-highlighting.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
