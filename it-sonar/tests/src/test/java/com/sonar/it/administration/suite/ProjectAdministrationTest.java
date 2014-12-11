/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.permissions.PermissionParameters;
import org.sonar.wsclient.qualitygate.*;
import org.sonar.wsclient.services.ProjectDeleteQuery;
import org.sonar.wsclient.services.PropertyQuery;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.user.UserParameters;

import javax.annotation.Nullable;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.fest.assertions.Assertions.assertThat;

public class ProjectAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  private static final String PROJECT_KEY = "sample";
  private static final String FILE_KEY = "sample:src/main/java/sample/Sample.java";

  @Before
  public void deleteAnalysisData() throws SQLException {
    orchestrator.resetData();
  }

  @Test
  public void delete_project_by_web_service() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNotNull();

    orchestrator.getServer().getAdminWsClient().delete(ProjectDeleteQuery.create(PROJECT_KEY));

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNull();
  }

  @Test(expected = ConnectionException.class)
  public void delete_only_projects() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();
    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(FILE_KEY))).isNotNull();

    // it's forbidden to delete only some files
    orchestrator.getServer().getAdminWsClient().delete(ProjectDeleteQuery.create(FILE_KEY));
  }

  @Test(expected = ConnectionException.class)
  public void admin_role_should_be_required_to_delete_project() {
    scanSampleWithDate("2012-01-01");

    assertThat(orchestrator.getServer().getWsClient().find(ResourceQuery.create(PROJECT_KEY))).isNotNull();

    // use getWsClient() instead of getAdminWsClient()
    orchestrator.getServer().getWsClient().delete(ProjectDeleteQuery.create(PROJECT_KEY));
  }

  /**
   * Test updated for SONAR-3570 and SONAR-5923
   */
  @Test
  public void project_deletion() throws Exception {
    String projectAdminUser = "project-deletion-with-admin-permission-on-project";
    SonarClient adminClient = orchestrator.getServer().adminWsClient();
    try {
      SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"));
      orchestrator.executeBuild(scan);

      // Create user having admin permission on previously analysed project
      adminClient.userClient().create(
        UserParameters.create().login(projectAdminUser).name(projectAdminUser).password("password").passwordConfirmation("password"));
      adminClient.permissionClient().addPermission(
        PermissionParameters.create().user(projectAdminUser).component("sample").permission("admin"));

      orchestrator.executeSelenese(
        Selenese.builder().setHtmlTestsInClasspath("project-deletion", "/selenium/administration/project-deletion/project-deletion.html").build()
        );
    } finally {
      adminClient.userClient().deactivate(projectAdminUser);
    }
  }

  @Test
  public void project_administration() throws Exception {
    GregorianCalendar today = new GregorianCalendar();

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build.setProperty("sonar.projectDate", (today.get(Calendar.YEAR) - 1) + "-01-01"));
    // The analysis must be run once again to have an history so that it is possible to delete a snapshot
    orchestrator.executeBuild(build.setProperty("sonar.projectDate", (today.get(Calendar.YEAR)) + "-01-01"));

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("project-administration",
        "/selenium/administration/project-administration/project-exclusions.html",
        "/selenium/administration/project-administration/project-general-exclusions.html",
        "/selenium/administration/project-administration/project-test-exclusions.html",
        "/selenium/administration/project-administration/project-general-test-exclusions.html",
        "/selenium/administration/project-administration/project-links.html",
        "/selenium/administration/project-administration/project-modify-versions.html",
        "/selenium/administration/project-administration/project-rename-current-version.html",
        "/selenium/administration/project-administration/project-history-deletion.html", // SONAR-3206
        "/selenium/administration/project-administration/project-quality-profile.html" // SONAR-3517
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4203
  @Test
  public void delete_version_of_multimodule_project() throws Exception {
    GregorianCalendar today = new GregorianCalendar();
    SonarRunner build = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-multi-modules-sample"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.projectDate", (today.get(Calendar.YEAR) - 1) + "-01-01");
    orchestrator.executeBuild(build);

    // The analysis must be run once again to have an history so that it is possible
    // to set/delete version on old snapshot
    build.setProperty("sonar.projectDate", today.get(Calendar.YEAR) + "-01-01");
    orchestrator.executeBuild(build);

    // There are 7 modules
    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(7);

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("delete_version_of_multimodule_project",
        "/selenium/administration/project-administration/multimodule-project-modify-version.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(14);

    selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("delete_version_of_multimodule_project",
        "/selenium/administration/project-administration/multimodule-project-delete-version.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    assertThat(count("events where category='Version'")).as("Different number of events").isEqualTo(7);
  }

  // SONAR-3326
  @Test
  public void display_alerts_correctly_in_history_page() throws Exception {
    QualityGateClient qgClient = orchestrator.getServer().adminWsClient().qualityGateClient();
    QualityGate qGate = qgClient.create("AlertsForHistory");
    qgClient.setDefault(qGate.id());

    // with this configuration, project should have an Orange alert
    QualityGateCondition lowThresholds = qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("lines").operator("GT").warningThreshold("5").errorThreshold("50"));
    scanSampleWithDate("2012-01-01");
    // with this configuration, project should have a Green alert
    qgClient.updateCondition(UpdateCondition.create(lowThresholds.id()).metricKey("lines").operator("GT").warningThreshold("5000").errorThreshold("5000"));
    scanSampleWithDate("2012-01-02");

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("display-alerts-history-page",
        "/selenium/administration/display-alerts-history-page/should-display-alerts-correctly-history-page.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    qgClient.unsetDefault();
    qgClient.destroy(qGate.id());
  }

  /**
   * SONAR-1352
   */
  @Test
  public void display_period_alert_on_project_dashboard() throws Exception {
    QualityGateClient qgClient = orchestrator.getServer().adminWsClient().qualityGateClient();
    QualityGate qGate = qgClient.create("AlertsForDashboard");
    qgClient.createCondition(NewCondition.create(qGate.id()).metricKey("lines").operator("LT").warningThreshold("0").errorThreshold("10")
      .period(1));
    qgClient.setDefault(qGate.id());

    // No alert
    scanSampleWithDate("2012-01-01");

    // Red alert because lines number has not changed since previous analysis
    scanSample();

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("display-period-alerts",
        "/selenium/administration/display-alerts/should-display-period-alerts-correctly.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    qgClient.unsetDefault();
    qgClient.destroy(qGate.id());
  }

  /**
   * SONAR-3425
   */
  @Test
  public void project_settings() {
    scanSampleWithDate("2012-01-01");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("project-settings",
      // SONAR-3425
      "/selenium/administration/project-settings/override-global-settings.html",

      "/selenium/administration/project-settings/only-on-project-settings.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    assertThat(orchestrator.getServer().getAdminWsClient().find(PropertyQuery.createForResource("sonar.skippedModules", "sample")).getValue())
      .isEqualTo("my-excluded-module");
  }

  /**
   * SONAR-1608
   */
  @Test
  public void bulk_update_project_keys() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("project-bulk-update-keys",
        "/selenium/administration/project-update-keys/bulk-update-impossible-because-duplicate-keys.html",
        "/selenium/administration/project-update-keys/bulk-update-impossible-because-no-input.html",
        "/selenium/administration/project-update-keys/bulk-update-impossible-because-no-match.html",
        "/selenium/administration/project-update-keys/bulk-update-success.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-1608
   */
  @Test
  public void fine_grain_update_project_keys() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/multi-modules-sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("project-fine-grained-update-keys",
        "/selenium/administration/project-update-keys/fine-grained-update-impossible.html",
        "/selenium/administration/project-update-keys/fine-grained-update-success.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-3956
   * SONAR-4827
   * Disabled because of false-positives -> require permissions WS (or loading spinner ?)
   */
  @Test
  @Ignore
  public void manage_permissions() {
    scanSample();

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("manage-permissions",
        "/selenium/administration/manage_project_roles/change_roles_of_users.html",
        "/selenium/administration/manage_project_roles/change_roles_of_groups.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4050
   * Disabled because of false-positives -> require permissions WS (or loading spinner ?)
   */
  @Test
  @Ignore
  public void do_not_reset_default_project_roles() {
    scanSample();

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("do_not_reset_default_roles_1",
        "/selenium/administration/do_not_reset_default_roles/1_set_project_roles.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    scanSample();

    selenese = Selenese.builder()
      .setHtmlTestsInClasspath("do_not_reset_default_roles_2",
        "/selenium/administration/do_not_reset_default_roles/2_project_roles_are_unchanged.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void anonymous_should_have_user_role_to_access_project() {
    scanSample();

    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("anonymous_should_have_user_role_to_access_project",
        "/selenium/administration/anonymous_should_have_user_role_to_access_project/remove_user_role.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4060
   */
  @Test
  public void display_module_settings() {
    orchestrator.executeBuild(MavenBuild.create(ItUtils.locateProjectPom("maven/modules-declaration"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("module-settings",
      // SONAR-3425
      "/selenium/administration/module-settings/display-module-settings.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  private void scanSample(@Nullable String date, @Nullable String profile) {
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/sample"))
      .setProperties("sonar.cpd.skip", "true");
    if (date != null) {
      scan.setProperty("sonar.projectDate", date);
    }
    if (profile != null) {
      scan.setProfile(profile);
    }
    orchestrator.executeBuild(scan);
  }

  private void scanSampleWithDate(String date) {
    scanSample(date, null);
  }

  private void scanSample() {
    scanSample(null, null);
  }

  private int count(String condition) {
    return orchestrator.getDatabase().countSql("select count(*) from " + condition);
  }

}
