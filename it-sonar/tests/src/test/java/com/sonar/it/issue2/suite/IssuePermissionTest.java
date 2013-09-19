/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.issue2.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;

public class IssuePermissionTest extends AbstractIssueTestCase2 {

  public static final String WITH_CODE_VIEWER_PERMISSION = "with-code-viewer-permission";
  public static final String WITHOUT_CODE_VIEWER_PERMISSION = "without-code-viewer-permission";

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @AfterClass
  public static void deactivateUsers() {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    client.userClient().deactivate(WITH_CODE_VIEWER_PERMISSION);
    client.userClient().deactivate(WITHOUT_CODE_VIEWER_PERMISSION);
  }

  @Test
  public void need_code_viewer_permission_to_see_source_code_from_issue() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/one-issue-per-line-profile.xml"));
    SonarRunner sampleProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.dynamicAnalysis", "false",
        // Change project key and name because project permissions cannot be rest by WS -> Use unique project key / name to have no conflict with other tests
        "sonar.projectKey", "sample-issue-code-viewer-permission", "sonar.projectName", "sample-issue-code-viewer-permission")
      .setRunnerVersion("2.2.2")
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(sampleProject);

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    UserParameters newUser = UserParameters.create().login(WITH_CODE_VIEWER_PERMISSION).name(WITH_CODE_VIEWER_PERMISSION)
      .password("password").passwordConfirmation("password");
    client.userClient().create(newUser);
    newUser = UserParameters.create().login(WITHOUT_CODE_VIEWER_PERMISSION).name(WITHOUT_CODE_VIEWER_PERMISSION)
      .password("password").passwordConfirmation("password");
    client.userClient().create(newUser);

    // Use Selenium to grant permissions to user as there's currently no WS to do that : remove all users/groups code viewer permission on project and the permission to one user
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("initialize-code-viewer-permissions-on-project",
      "/selenium/issue/issue-permissions/initialize-code-viewer-permissions-on-project.html")
      .build());

    // Now the real test begins!
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("need-code-viewer-permission-to-see-source-code-from-issue",
      "/selenium/issue/issue-permissions/without-code-viewer-permission-source-code-from-issue-is-hidden.html",
      "/selenium/issue/issue-permissions/with-code-viewer-permission-source-code-from-issue-is-visible.html"
    ).build());
  }

}
