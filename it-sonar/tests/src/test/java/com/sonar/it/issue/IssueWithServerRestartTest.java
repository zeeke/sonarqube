/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.fest.assertions.Assertions.assertThat;

public class IssueWithServerRestartTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.locateTestPlugin("deprecated-xoo-rule-plugin"))
    .build();

  /**
   * SONAR-4364
   */
  @Test
  public void scan_should_close_issue_on_more_existing_rule() throws Exception {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();

    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-deprecated-rule-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("with-deprecated-rule");
    orchestrator.executeBuild(scan);

    Issue issue = issueClient.find(IssueQuery.create().rules("deprecated-repo:deprecated-rule")).list().get(0);
    assertThat(issue.status()).isEqualTo("OPEN");
    assertThat(issue.resolution()).isNull();

    // Remove deprecated rule plugin with updatecenter web console because there's no way to do that with the Orchestrator API.
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("remove-rule-plugin", "/selenium/issue/remove-deprecated-rule-plugin.html").build()
    );
    orchestrator.restartSonar();

    // FIXME Ignored as it fails on Jenkins
//    check_removed_rules_do_not_prevent_displaying_issues_code_viewer();

    // Re analyse the project in order to modify the status of the issue
    orchestrator.executeBuild(scan);

    issue = issueClient.find(IssueQuery.create().rules("deprecated-repo:deprecated-rule")).list().get(0);
    assertThat(issue.status()).isEqualTo("CLOSED");
    assertThat(issue.resolution()).isEqualTo("REMOVED");
  }

  @Test
  public void scanning_a_second_project_should_not_remove_issues_of_first_project() throws Exception {
    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();

    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));

    // Scan the first project and count number of issues
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(scan);
    int issues = issueClient.find(IssueQuery.create().components("sample")).list().size();
    assertThat(issues).isGreaterThan(0);

    // Scan another project
    orchestrator.executeBuilds(SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample")).setProfile("one-issue-per-line"));

    // Re scan first project, number of its issues should be the same
    orchestrator.executeBuild(scan);
    assertThat(issueClient.find(IssueQuery.create().components("sample")).list().size()).isEqualTo(issues);
  }

}
