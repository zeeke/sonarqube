/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests the extension point IssueFilter
 */
public class IssueFilterExtensionTest extends AbstractIssueTestCase {

  @Before
  public void resetData() {
    orchestrator.resetData();
  }

  @Test
  public void should_filter_files() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));

    MavenBuild scan = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .setCleanSonarGoals()
      .setProperties("sonar.exclusions", "**/HelloA1.java")
      .setProfile("issues");
    orchestrator.executeBuild(scan);

    Issues issues = search(IssueQuery.create());
    assertThat(issues.list()).isNotEmpty();
    int count = 0;
    for (Issue issue : issues.list()) {
      assertThat(issue.componentKey()).doesNotContain("HelloA1");
      count++;
    }

    assertThat(ItUtils.getMeasure(orchestrator, "com.sonarsource.it.samples:multi-modules-sample", "violations").getIntValue()).isEqualTo(count);
  }

  @Test
  public void should_filter_issues() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/issues.xml"));

    // Disable issue filters
    MavenBuild scan = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .setCleanSonarGoals()
      .setProperties("enableIssueFilters", "false")
      // Force language to fix issue with Common Rules
      .setProfile("issues");
    orchestrator.executeBuild(scan);

    // Issue filter removes issues on lines < 5
    // Deprecated violation filter removes issues detected by PMD
    IssueClient issueClient = ItUtils.newWsClientForAnonymous(orchestrator).issueClient();
    Issues unresolvedIssues = issueClient.find(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample").resolved(false));
    int issuesBeforeLine5 = countIssuesBeforeLine5(unresolvedIssues.list());
    int pmdIssues = countPmdIssues(unresolvedIssues.list());
    assertThat(issuesBeforeLine5).isGreaterThan(0);
    assertThat(pmdIssues).isGreaterThan(0);

    // Enable issue filters
    scan = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .setCleanSonarGoals()
      .setProperties("enableIssueFilters", "true")
      .setProfile("issues");
    orchestrator.executeBuild(scan);

    unresolvedIssues = issueClient.find(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample").resolved(false));
    Issues resolvedIssues = issueClient.find(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample").resolved(true));
    assertThat(countIssuesBeforeLine5(unresolvedIssues.list())).isZero();
    assertThat(countPmdIssues(unresolvedIssues.list())).isZero();
    assertThat(countIssuesBeforeLine5(resolvedIssues.list())).isGreaterThan(0);
    assertThat(countPmdIssues(resolvedIssues.list())).isGreaterThan(0);
  }

  private int countPmdIssues(List<Issue> issues) {
    int count = 0;
    for (Issue issue : issues) {
      if (issue.ruleKey().startsWith("pmd:")) {
        count++;
      }
    }
    return count;
  }

  private int countIssuesBeforeLine5(List<Issue> issues) {
    int count = 0;
    for (Issue issue : issues) {
      if (issue.line() != null && issue.line() < 5 && !issue.ruleKey().startsWith("pmd:")) {
        count++;
      }
    }
    return count;
  }
}
