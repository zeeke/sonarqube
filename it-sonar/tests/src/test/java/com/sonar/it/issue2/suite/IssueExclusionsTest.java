package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import static org.fest.assertions.Assertions.assertThat;

public class IssueExclusionsTest extends AbstractIssueTestCase2 {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-exclusions";
  private static final String PROJECT_DIR = "issue/exclusions";

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @AfterClass
  public static void purgeManualRules(){
    deleteManualRules();
  }

  @Test
  public void should_ignore_all_files() {
    scan(
      "sonar.issue.ignore.multicriteria", "1",
      "sonar.issue.ignore.multicriteria.1.resourceKey", "**/*.xoo",
      "sonar.issue.ignore.multicriteria.1.ruleKey", "*",
      "sonar.issue.ignore.multicriteria.1.lineRange", "*"
      );

    assertThat(searchIssuesByComponent(PROJECT_KEY)).hasSize(7);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "violations", "info_violations", "minor_violations", "major_violations",
      "blocker_violations", "critical_violations", "violations_density"));
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(7);
    assertThat(project.getMeasureIntValue("info_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("minor_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("major_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("blocker_violations")).isEqualTo(0);
    assertThat(project.getMeasureIntValue("critical_violations")).isEqualTo(7);
  }

  protected void scan(String... properties) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTest/with-many-rules.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir(PROJECT_DIR))
      .setProperties("sonar.cpd.skip", "true")
      .setProperties(properties)
      .setProfile("with-many-rules")
      // Multi module project have to use sonar-runner 2.2.2 to not fail
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(scan);
  }
}
