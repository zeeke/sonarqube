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
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4308
   */
  @Test
  public void delete_all_closed_issues() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-many-rules.xml"));

    // Generate some issues
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false")
      .setProfile("with-many-rules"));

    // All the issues are open
    List<Issue> issues = search(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }

    // Second scan with empty profile -> all issues are resolve and closed -> deleted by purge because property value is zero
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "0"));
    issues = search(IssueQuery.create()).list();
    assertThat(issues).isEmpty();
  }

  /**
   * SONAR-4308
   */
  @Test
  public void purge_old_closed_issues() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-many-rules.xml"));

    // Generate some issues
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", "2010-01-01")
      .setProfile("with-many-rules"));

    // All the issues are open
    List<Issue> issues = search(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNull();
    }

    // Second scan with empty profile -> all issues are resolve and closed
    // -> Not delete because less than 30 days long
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", "2010-01-10", "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "30"));
    issues = search(IssueQuery.create()).list();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isNotNull();
      assertThat(issue.status()).isEqualTo("CLOSED");
    }

    // Third scan much later -> closed issues are deleted
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.projectDate", "2013-01-10", "sonar.dbcleaner.daysBeforeDeletingClosedIssues", "30"));
    issues = search(IssueQuery.create()).list();
    assertThat(issues.isEmpty());
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

    int issuesOnModuleB = search(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_b")).list().size();

    // Second scan without module B -> issues on module B are resolved as removed and closed
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.dynamicAnalysis", "false", "sonar.modules", "module_a")
      .setProfile("with-many-rules"));

    // Resolved should should all be mark as REMOVED and affect to module b
    issues = search(IssueQuery.create().resolved(true)).list();
    for (Issue issue : issues) {
      assertThat(issue.resolution()).isEqualTo("REMOVED");
      assertThat(issue.status()).isEqualTo("CLOSED");
      assertThat(issue.componentKey()).contains("com.sonarsource.it.samples:multi-modules-sample:module_b");
    }

    assertThat(issues).hasSize(issuesOnModuleB);
  }
}
