/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.permission.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.*;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.user.UserParameters;

public class IssuePermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = PermissionTestSuite.ORCHESTRATOR;


  private static final String WITH_CODE_VIEWER_PERMISSION = "with-code-viewer-permission";
  private static final String WITHOUT_CODE_VIEWER_PERMISSION = "without-code-viewer-permission";

  @BeforeClass
  public static void init() {
    orchestrator.getDatabase().truncateInspectionTables();

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/permission/one-issue-per-line-profile.xml"));
    SonarRunner sampleProject = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .withoutDynamicAnalysis()
      .setProfile("one-issue-per-line");
    orchestrator.executeBuild(sampleProject);

    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    client.userClient().create(UserParameters.create().login(WITH_CODE_VIEWER_PERMISSION).name(WITH_CODE_VIEWER_PERMISSION)
      .password("password").passwordConfirmation("password"));
    client.permissionClient().addPermission(PermissionParameters.create().user(WITH_CODE_VIEWER_PERMISSION).component("sample").permission("codeviewer"));

    client.userClient().create(UserParameters.create().login(WITHOUT_CODE_VIEWER_PERMISSION).name(WITHOUT_CODE_VIEWER_PERMISSION)
      .password("password").passwordConfirmation("password"));
    client.permissionClient().removePermission(PermissionParameters.create().group("anyone").component("sample").permission("codeviewer"));
  }

  @AfterClass
  public static void deactivateUsers() {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    client.userClient().deactivate(WITH_CODE_VIEWER_PERMISSION);
    client.userClient().deactivate(WITHOUT_CODE_VIEWER_PERMISSION);
  }

  /**
   * SONAR-4686
   */
  @Test
  public void need_code_viewer_permission_to_see_source_code_from_issue_detail() {
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("need-code-viewer-permission-to-see-source-code-from-issue-detail",
      "/selenium/permission/issue-permissions/without-code-viewer-permission-source-code-from-issue-detail-is-hidden.html",
      "/selenium/permission/issue-permissions/with-code-viewer-permission-source-code-from-issue-detail-is-visible.html"
    ).build());
  }

  // TODO
  @Test
  @Ignore
  public void need_browse_permission_to_see_issue() {

  }

  // TODO
  @Test
  @Ignore
  public void need_browse_permission_to_see_issue_changelog() {

  }
}
