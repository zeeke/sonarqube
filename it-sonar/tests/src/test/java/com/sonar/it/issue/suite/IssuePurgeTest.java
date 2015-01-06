/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssuePurgeTest extends AbstractIssueTestCase {

  @Before
  public void deleteAnalysisData() {
    orchestrator.resetData();
    // reset settings before test
    ItUtils.setServerProperty(orchestrator, "sonar.dbcleaner.daysBeforeDeletingClosedIssues", null);
  }

  /**
   * SONAR-4308
   */
  @Test
  public void purge_old_closed_issues() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-many-rules.xml"));
    ItUtils.setServerProperty(orchestrator, "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "5000");

    // Generate some issues
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", "2014-10-01")
      .setProfile("with-many-rules"));

    // All the issues are open
    List<Issue> issues = search(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }

    // Second scan with empty profile -> all issues are resolved and closed
    // -> Not deleted because less than 5000 days long
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", "2014-10-15"));
    issues = search(IssueQuery.create()).list();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNotNull();
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    // Third scan -> closed issues are deleted
    ItUtils.setServerProperty(orchestrator, "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "1");
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", "2014-10-20"));
    issues = search(IssueQuery.create()).list();

    assertThat(issues).isEmpty();
  }

  /**
   * SONAR-5200
   */
  @Test
  public void resolve_issues_when_removing_module() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-many-rules.xml"));

    // Generate some issues
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProfile("with-many-rules"));

    // All the issues are open
    List<Issue> issues = search(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }
    Issue issue = issues.get(0);

    int issuesOnModuleB = search(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample:module_b")).list().size();
    assertThat(issuesOnModuleB).isEqualTo(28);

    // Second scan without module B -> issues on module B are resolved as removed and closed
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.modules", "module_a")
      .setProfile("with-many-rules"));

    // Resolved should should all be mark as REMOVED and affect to module b
    List<Issue> reloadedIssues = search(IssueQuery.create().resolved(true)).list();
    assertThat(reloadedIssues.size()).isGreaterThan(0);
    assertThat(reloadedIssues).hasSize(issuesOnModuleB);
    for (Issue reloadedIssue : reloadedIssues) {
      assertThat(reloadedIssue.resolution()).isEqualTo("REMOVED");
      assertThat(reloadedIssue.status()).isEqualTo("CLOSED");
      assertThat(reloadedIssue.componentKey()).contains("com.sonarsource.it.samples:multi-modules-sample:module_b");
      assertThat(reloadedIssue.updateDate().before(issue.updateDate())).isFalse();
      assertThat(reloadedIssue.closeDate()).isNotNull();
      assertThat(reloadedIssue.closeDate().before(reloadedIssue.creationDate())).isFalse();
    }
  }
}
