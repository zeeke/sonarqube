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
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import static org.fest.assertions.Assertions.assertThat;

public class ProfileTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin", "2.1"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1"))
    .build();

  @Before
  public void deleteData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void test_backup() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java-profile.xml"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-backup",
      "/selenium/profile/backup-profile.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_export() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Sonar_way_java-profile.xml"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-export",
      "/selenium/profile/profile-export/permalink-to-sonar-configuration.html",
      "/selenium/profile/profile-export/permalink-to-checkstyle-configuration.html",
      "/selenium/profile/profile-export/permalink-to-default-checkstyle-configuration.html",
      "/selenium/profile/profile-export/permalink-to-pmd-configuration.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_management_of_profiles() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java-profile.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Sonar_way_java-profile.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Sun_checks_java-profile.xml"));

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
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/one-rule-profile.xml"));
    setProperty("sonar.profile.xoo", "One Rule Profile");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("default-profile",
      "/selenium/profile/should-not-delete-default-profile.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void associate_profile_to_project() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java-profile.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.profile.java", "IT");
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-relationship-with-projects",
      "/selenium/profile/dashboard_links_to_used_profile.html",
      "/selenium/profile/link-profile-to-project.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_override_profile_with_property() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java-profile.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Overridden_java.xml"));

    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.profile.java", "Overridden");
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
      .setProperty("sonar.profile.xoo", "xoo1");
    BuildResult result = orchestrator.executeBuild(build);

    assertThat(result.getLogs()).contains("Quality profile for xoo: xoo");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("change-project-profile-from-xoo1-to-xoo2",
      "/selenium/profile/change-project-profile-from-xoo1-to-xoo2.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    build = configureRunner("shared/xoo-sample")
      // Use profile defined in UI
      .setProfile("");
    result = orchestrator.executeBuild(build);
    assertThat(result.getLogs()).contains("Quality profile for xoo: xoo2");
  }

  @Test
  public void test_comparison() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Comparison/Sonar_way_java.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Comparison/To_compare_-_one_java.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Comparison/To_compare_-_two_java.xml"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-comparison",
      "/selenium/profile/comparison/compare-profiles.html",
      "/selenium/profile/comparison/compare-same-profile.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-2124
  @Test
  public void test_inheritance() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Inheritance/Child_Profile_java.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Inheritance/Parent_Profile_java.xml"));

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("set-parent-profile-to-child-profile",
      "/selenium/profile/inheritance/set-parent-profile-to-child-profile.html"
      ).build());

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
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/one-rule-profile.xml"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("profile-rule-notes",
      "/selenium/profile/rule-notes/check-no-action-if-not-authenticated.html",
      // SONAR-3382, SONAR-4657
      "/selenium/profile/rule-notes/extend-description-and-remove-it.html",
      "/selenium/profile/rule-notes/add-delete-note-on-active-rule.html",
      "/selenium/profile/rule-notes/cant-add-note-on-inactive-rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4107
   */
  @Test
  public void should_not_delete_all_associations_when_deleting_a_profile() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/IT_java-profile.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Sonar_way_java-profile.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileTest/Sun_checks_java-profile.xml"));
    setProperty("sonar.profile.java", "IT");

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

  private SonarRunner configureRunner(String projectPath, String... props) {
    SonarRunner runner = SonarRunner.create(ItUtils.locateProjectDir(projectPath))
      .setProperties(props);
    return runner;
  }

  static void setProperty(String key, String value) {
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery(key, value));
  }
}
