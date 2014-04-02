/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.maven.it.suite;

import com.sonar.maven.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTest extends AbstractMavenTest {

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void shouldSupportJapaneseCharset() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("java/japanese-charset"))
      .setGoals(cleanSonarGoal());
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("java-japanese-charset",
      "/selenium/java-japanese-charset/japanese_duplications.html",
      "/selenium/java-japanese-charset/japanese_sources.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-3726 & SONAR-3823
  @Test
  public void shouldNotActivateJavaRelatedExtensionsIfNotJavaProject() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("java/non-java-project")).setGoals(cleanSonarGoal());
    String logs = orchestrator.executeBuild(build).getLogs();

    assertThat(logs).doesNotContain("Cobertura");
    assertThat(logs).doesNotContain("Checkstyle");
  }

  // SONAR-3893 & SONAR-4247
  @Test
  public void shouldHighlightJavaSourceCode() throws Exception {

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample-with-tests"))
      .setGoals(cleanSonarGoal())
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
      .setGoals(cleanSonarGoal())
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("highlight-symbol-usages",
      "/selenium/java/symbol-usages-highlighting.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
