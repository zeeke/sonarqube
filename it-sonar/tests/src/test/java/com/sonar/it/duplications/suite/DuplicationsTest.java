/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.duplications.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DuplicationsTest {

  @ClassRule
  public static Orchestrator orchestrator = DuplicationsTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("duplications/file-duplications"))
      .setCleanPackageSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void duplicated_lines_within_same_class() {
    Resource file = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_within_same_class/DuplicatedLinesInSameClass.java");
    assertThat(file, not(nullValue()));
    assertThat(file.getMeasureValue("duplicated_blocks"), is(2.0));
    assertThat(file.getMeasureValue("duplicated_lines"), is(27.0 * 2)); // 2 blocks with 27 lines
    assertThat(file.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file.getMeasureValue("duplicated_lines_density"), is(60.0));

    // SONAR-5765
    assertThat(file.getMeasureValue("useless-duplicated-lines"), is(27.0));
  }

  @Test
  public void duplicated_same_lines_within_3_classes() {
    Resource file1 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_same_lines_within_3_classes/Class1.java");
    assertThat(file1, not(nullValue()));
    assertThat(file1.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines"), is(29.0));
    assertThat(file1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines_density"), is(47.5));

    Resource file2 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_same_lines_within_3_classes/Class2.java");
    assertThat(file2, not(nullValue()));
    assertThat(file2.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines"), is(29.0));
    assertThat(file2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines_density"), is(48.3));

    Resource file3 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_same_lines_within_3_classes/Class3.java");
    assertThat(file3, not(nullValue()));
    assertThat(file3.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file3.getMeasureValue("duplicated_lines"), is(29.0));
    assertThat(file3.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file3.getMeasureValue("duplicated_lines_density"), is(46.0));

    Resource pkg = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_same_lines_within_3_classes");
    assertThat(pkg, not(nullValue()));
    assertThat(pkg.getMeasureValue("duplicated_blocks"), is(3.0));
    assertThat(pkg.getMeasureValue("duplicated_lines"), is(29.0 * 3)); // 3 blocks with 29 lines
    assertThat(pkg.getMeasureValue("duplicated_files"), is(3.0));
    assertThat(pkg.getMeasureValue("duplicated_lines_density"), is(47.3));
  }

  @Test
  public void duplicated_lines_within_package() {
    Resource file1 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_within_package/DuplicatedLinesInSamePackage1.java");
    assertThat(file1, not(nullValue()));
    assertThat(file1.getMeasureValue("duplicated_blocks"), is(4.0));
    assertThat(file1.getMeasureValue("duplicated_lines"), is(72.0));
    assertThat(file1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines_density"), is(58.1));

    Resource file2 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_within_package/DuplicatedLinesInSamePackage2.java");
    assertThat(file2, not(nullValue()));
    assertThat(file2.getMeasureValue("duplicated_blocks"), is(3.0));
    assertThat(file2.getMeasureValue("duplicated_lines"), is(58.0));
    assertThat(file2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines_density"), is(64.4));

    Resource pkg = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_within_package");
    assertThat(pkg, not(nullValue()));
    assertThat(pkg.getMeasureValue("duplicated_blocks"), is(4.0 + 3.0));
    assertThat(pkg.getMeasureValue("duplicated_lines"), is(72.0 + 58.0));
    assertThat(pkg.getMeasureValue("duplicated_files"), is(2.0));
    assertThat(pkg.getMeasureValue("duplicated_lines_density"), is(60.7));
  }

  @Test
  public void duplicated_lines_with_other_package() {
    Resource file1 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_with_other_package1/DuplicatedLinesWithOtherPackage.java");
    assertThat(file1, not(nullValue()));
    assertThat(file1.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(file1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines_density"), is(60.0));

    Resource pkg1 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_with_other_package1");
    assertThat(pkg1, not(nullValue()));
    assertThat(pkg1.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(pkg1.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(pkg1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(pkg1.getMeasureValue("duplicated_lines_density"), is(60.0));

    Resource file2 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_with_other_package2/DuplicatedLinesWithOtherPackage.java");
    assertThat(file2, not(nullValue()));
    assertThat(file2.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(file2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines_density"), is(60.0));

    Resource pkg2 = getResource("com.sonarsource.it.samples:duplications:src/main/java/duplicated_lines_with_other_package2");
    assertThat(pkg2, not(nullValue()));
    assertThat(pkg2.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(pkg2.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(pkg2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(pkg2.getMeasureValue("duplicated_lines_density"), is(60.0));
  }

  @Test
  public void consolidation() {
    Resource project = getResource("com.sonarsource.it.samples:duplications");
    assertThat(project, not(nullValue()));
    assertThat(project.getMeasureValue("duplicated_blocks"), is(14.0));
    assertThat(project.getMeasureValue("duplicated_lines"), is(343.0));
    assertThat(project.getMeasureValue("duplicated_files"), is(8.0));
    assertThat(project.getMeasureValue("duplicated_lines_density"), is(56.4));
  }

  @Test
  public void drilldown() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("drilldown",
      "/selenium/duplications/drilldown-without-ratio.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3060
   */
  @Test
  public void hugeFile() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("huge-file"))
      // Fail with OOM during SourcePersister
      .setEnvironmentVariable("MAVEN_OPTS", "-Xmx600m")
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);
    Resource file = getResource("com.sonarsource.it.samples:huge-file:src/main/java/huge/HugeFile.java");
    assertThat(file.getMeasureValue("duplicated_lines"), greaterThan(50000.0));
  }

  /**
   * SONAR-3108
   */
  @Test
  public void use_duplication_exclusions() {
    // Use a new project key to avoid conflict woth other tests
    String projectKey = "com.sonarsource.it.samples:duplications-with-exclusions";
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("duplications/file-duplications"))
      .setCleanPackageSonarGoals()
      .setProperties("sonar.projectKey", projectKey,
        "sonar.cpd.exclusions", "**/Class*");
    orchestrator.executeBuild(build);

    Resource project = getResource(projectKey);
    assertThat(project, not(nullValue()));
    assertThat(project.getMeasureValue("duplicated_blocks"), is(11.0));
    assertThat(project.getMeasureValue("duplicated_lines"), is(256.0));
    assertThat(project.getMeasureValue("duplicated_files"), is(5.0));
    assertThat(project.getMeasureValue("duplicated_lines_density"), is(42.1));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(key, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density", "useless-duplicated-lines"));
  }

}
