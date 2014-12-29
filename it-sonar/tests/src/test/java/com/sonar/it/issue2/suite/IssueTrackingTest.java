/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue2.suite;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueTrackingTest extends AbstractIssueTestCase2 {

  private static final String OLD_DATE = "2012-01-01";
  private static final String NEW_DATE = "2014-05-18";

  @Before
  public void deleteAnalysisData() {
    orchestrator.resetData();
  }

  /**
   * SONAR-3072
   */
  @Test
  public void track_issues_based_on_blocks_recognition() throws Exception {
    // The PMD rule System.out is enabled
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTrackingTest/issue-tracking-profile.xml"));

    // version 1
    MavenBuild firstScan = MavenBuild.create(ItUtils.locateProjectPom("issue/tracking-v1"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", OLD_DATE)
      .setProfile("issue-tracking");

    // version 2
    MavenBuild secondScan = MavenBuild.create(ItUtils.locateProjectPom("issue/tracking-v2"))
      .setCleanSonarGoals()
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", NEW_DATE)
      .setProfile("issue-tracking");

    orchestrator.executeBuilds(firstScan);
    orchestrator.executeBuilds(secondScan);

    List<Issue> issues = searchUnresolvedIssuesByComponent("com.sonarsource.it.samples:issue-tracking:src/SystemOut.java");
    assertThat(issues).hasSize(4);

    // issues created during the first scan and moved during the second scan
    assertSameDate(getIssueOnLine(18, "pmd", "SystemPrintln", issues).creationDate(), OLD_DATE);
    assertSameDate(getIssueOnLine(22, "pmd", "SystemPrintln", issues).creationDate(), OLD_DATE);

    // issues created during the second scan
    assertSameDate(getIssueOnLine(10, "pmd", "SystemPrintln", issues).creationDate(), NEW_DATE);
    assertSameDate(getIssueOnLine(14, "pmd", "SystemPrintln", issues).creationDate(), NEW_DATE);
  }

  @Test
  public void track_issues_on_dry_run() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTrackingTest/issue-on-tag-foobar.xml"));

    // version 1
    SonarRunner firstScan = SonarRunner.create(ItUtils.locateProjectDir("issue/xoo-tracking-v1"))
      .setProperties("sonar.projectDate", OLD_DATE)
      .setProfile("issue-on-tag-foobar");

    // version 2, dry run
    SonarRunner secondScan = SonarRunner.create(ItUtils.locateProjectDir("issue/xoo-tracking-v2"))
      .setProperties("sonar.dryRun", "true", "sonar.projectDate", NEW_DATE)
      .setProfile("issue-on-tag-foobar");

    orchestrator.executeBuilds(firstScan, secondScan);

    // TODO it's not possible yet to make assertions on dry runs
    // This test only verifies that the dry run batch does not fail
    // when computing variations against reference scan.
  }

  /**
   * SONAR-4310
   */
  @Test
  public void track_existing_unchanged_issues_on_module() throws Exception {
    // The custom rule on module is enabled
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTrackingTest/one-issue-per-module-profile.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-module");
    orchestrator.executeBuild(scan);

    // Only one issue is created
    assertThat(search(IssueQuery.create()).list()).hasSize(1);
    Issue issue = searchRandomIssue();

    // Re analysis of the same project
    orchestrator.executeBuild(scan);

    // No new issue should be created
    assertThat(search(IssueQuery.create()).list()).hasSize(1);

    // The issue on module should stay open and be the same from the first analysis
    Issue reloadIssue = searchIssueByKey(issue.key());
    assertThat(reloadIssue.creationDate()).isEqualTo(issue.creationDate());
    assertThat(reloadIssue.status()).isEqualTo("OPEN");
    assertThat(reloadIssue.resolution()).isNull();
  }

  /**
   * SONAR-4310
   */
  @Test
  public void track_existing_unchanged_issues_on_multi_modules() throws Exception {
    // The custom rule on module is enabled
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/IssueTrackingTest/one-issue-per-module-profile.xml"));

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProfile("one-issue-per-module");
    orchestrator.executeBuild(scan);

    // One issue by module are created
    List<Issue> issues = search(IssueQuery.create()).list();
    assertThat(issues).hasSize(4);

    // Re analysis of the same project
    orchestrator.executeBuild(scan);

    // No new issue should be created
    assertThat(search(IssueQuery.create()).list()).hasSize(issues.size());

    // Issues on modules should stay open and be the same from the first analysis
    for (Issue issue : issues) {
      Issue reloadIssue = searchIssueByKey(issue.key());
      assertThat(reloadIssue.status()).isEqualTo("OPEN");
      assertThat(reloadIssue.resolution()).isNull();
      assertThat(reloadIssue.creationDate()).isEqualTo(issue.creationDate());
      assertThat(reloadIssue.updateDate()).isEqualTo(issue.updateDate());
    }
  }

  private Issue getIssueOnLine(final Integer line, final String repoKey, final String ruleKey, List<Issue> issues) {
    return Iterables.find(issues, new Predicate<Issue>() {
      public boolean apply(Issue issue) {
        return Objects.equal(issue.line(), line) &&
          Objects.equal(issue.ruleKey(), repoKey + ":" + ruleKey);
      }
    });
  }

  private void assertSameDate(Date date1, String date2Text) throws ParseException {
    Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(date2Text);
    assertThat(DateUtils.isSameDay(date1, date2)).describedAs("Expected '" + date2Text + " ' but got '" + new SimpleDateFormat("yyyy-MM-dd").format(date1) + "'").isTrue();
  }

}
