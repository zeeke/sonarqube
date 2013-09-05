/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ProfileTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void test_backup() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/backup.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-backup",
      "/selenium/profile/backup-profile.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_export() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/backup.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-export",
      "/selenium/profile/profile-export/permalink-to-sonar-configuration.html",
      "/selenium/profile/profile-export/permalink-to-checkstyle-configuration.html",
      "/selenium/profile/profile-export/permalink-to-default-checkstyle-configuration.html",
      "/selenium/profile/profile-export/permalink-to-pmd-configuration.html",
      "/selenium/profile/profile-export/permalink-to-sonar-configuration-alerts.html" // SONAR-1352
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_management_of_profiles() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/backup.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-administration",
      "/selenium/profile/activate-profile.html",
      "/selenium/profile/bulk-change.html",
      "/selenium/profile/cancel-profile-creation.html",
      "/selenium/profile/copy-profile.html",
      "/selenium/profile/rename-profile.html",
      "/selenium/profile/create-and-delete-profile.html",
      "/selenium/profile/display-permalinks-to-tools.html",
      "/selenium/profile/do-not-copy-with-blank-name.html",
      "/selenium/profile/do-not-copy-with-existing-name.html",
      "/selenium/profile/do-not-create-profile-with-existing-name.html",
      "/selenium/profile/read-only-mode-when-anonymous-user.html",
      "/selenium/profile/should_import_checkstyle_findbugs_pmd.html",
      "/selenium/profile/SONAR-560_remove_a_rule_from_sonar_way.html",
      "/selenium/profile/user-profiles-are-editable.html",
      "/selenium/profile/copy_a_provided_profile_and_modify_a_rule_param.html",
      "/selenium/profile/SONAR-1000_quality_profile_with_space_or_dot.html"

      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_not_delete_default_profile() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/backup.xml")));
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("default-profile",
      "/selenium/profile/should-not-delete-default-profile.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_project_association() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProfile("IT");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-relationship-with-projects",
      "/selenium/profile/dashboard_links_to_used_profile.html",
      "/selenium/profile/link-profile-to-project.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_override_profile_with_property() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Overridden_java.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProfile("Overridden");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-override-with-property",
      "/selenium/profile/override-property-with-property.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4632
  @Test
  public void use_profile_defined_on_project_ui() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/xoo1.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/xoo2.xml"));

    SonarRunner build = configureRunner("shared/xoo-sample")
      .setProfile("xoo1");
    BuildResult result = orchestrator.executeBuild(build);

    assertThat(result.getLogs()).contains("Quality profile : [name=xoo1,language=xoo]");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("change-project-profile-from-xoo1-to-xoo2",
      "/selenium/profile/change-project-profile-from-xoo1-to-xoo2.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    build = configureRunner("shared/xoo-sample")
      // Use profile defined in UI
      .setProfile("");
    result = orchestrator.executeBuild(build);
    assertThat(result.getLogs()).contains("Quality profile : [name=xoo2,language=xoo]");

    // Just to be sure it also works with sonar-runner 2.2 (before moving reactor builder into sonar)
    build = configureRunner("shared/xoo-sample")
      .setRunnerVersion("2.2.2")
      // Use profile defined in UI
      .setProfile("");
    result = orchestrator.executeBuild(build);
    assertThat(result.getLogs()).contains("Quality profile : [name=xoo2,language=xoo]");
  }

  @Test
  public void test_comparison() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/compare-profiles.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-comparison",
      "/selenium/profile/comparison/compare-profiles.html",
      "/selenium/profile/comparison/compare-same-profile.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-2124
  @Test
  public void test_inheritance() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/parent-and-child-profiles.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-inheritance",
      "/selenium/profile/inheritance/check-inherited-rule.html",
      "/selenium/profile/inheritance/check-overridden-rule.html",
      "/selenium/profile/inheritance/revert-to-parent-definition.html",
      "/selenium/profile/inheritance/modify-parameter-from-inherited-profile.html",
      "/selenium/profile/inheritance/go-to-parent-definition.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-1492
  @Test
  public void test_rule_notes() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/one-rule-profile.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-rule-notes",
      "/selenium/profile/rule-notes/check-no-action-if-not-authenticated.html",
      // SONAR-3382
      "/selenium/profile/rule-notes/extend-description.html",
      // "/selenium/profile/rule-notes/add-delete-note-on-active-rule.html", disabled, fails on servers
      "/selenium/profile/rule-notes/cant-add-note-on-inactive-rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_alerts() {
    orchestrator.restoreSettings(FileUtils.toFile(getClass().getResource("/com/sonar/it/profile/ProfileTest/backup.xml")));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-alerts",
      "/selenium/profile/alerts/create_and_delete_alert_on_lines_of_code.html",
      // SONAR-1352
      "/selenium/profile/alerts/create_alert_with_period.html",
      // SONAR-1352
      "/selenium/profile/alerts/create_alert_with_period_on_new_metric.html",
      "/selenium/profile/alerts/should_validate_alert_on_creation.html",

      // SONAR-2983
      "/selenium/profile/alerts/boolean_criteria.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4107
   */
  @Test
  public void should_not_delete_all_associations_when_deleting_a_profile() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProfile("IT");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-deletion-associations",
      "/selenium/profile/SONAR-4107_delete_quality_profile_removes_all_associations.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_activate_a_rule() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/profile-empty.xml"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should-activate-a-rule",
      "/selenium/profile/activate-a-rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_deactivate_a_rule() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/profile-with-one-rule.xml"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should-deactivate-a-rule",
      "/selenium/profile/deactivate-a-rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Ignore("Requires http://jira.sonarsource.com/browse/ORCH-131")
  @Test
  public void should_not_restore_provided_profiles() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("provided-profiles", "/selenium/profile/delete-sonar-way.html").build();
    orchestrator.executeSelenese(selenese);

    // TODO restart and check that profile "sonar way" has not been restored
  }

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      .setRunnerVersion("2.3")
      .setProperties(props);
    return runner;
  }
}
