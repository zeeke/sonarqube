/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Event;
import org.sonar.wsclient.services.EventQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ProfileChangelogTest {

  private static final String SELENIUM_CATEGORY = "profile-changelog";

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin", "2.1-SNAPSHOT"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1-SNAPSHOT"))
    .build();

  @BeforeClass
  public static void restoreBackup() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/profile_a.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/profile_b.xml"));
  }

  @Before
  public void deleteAnalysisData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void should_create_changelog() {
    firstAnalysis();
    analyseWithNewProfile();
    analyseWithModifiedProfile();
    examineChangelog();
  }

  /**
   * SONAR-2621
   */
  @Test
  public void should_be_available_to_anonymous() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/should_be_available_to_anonymous.xml"));
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("should-be-available-to-anonymous", "/selenium/profile-changelog/anonymous-access.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-2986
   */
  @Test
  public void should_update_changelog_only_after_first_analysis() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/should_update_changelog_only_after_first_analysis.xml"));
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should-not-update-changelog-before-first-analysis",
      "/selenium/profile-changelog/should-not-update-changelog-before-first-analysis.html").build());

    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProperties("sonar.cpd.skip", "true")
      .setProfile("should_not_update_changelog_before_first_analysis");
    orchestrator.executeBuild(scan);

    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("should-update-changelog-after-first-analysis",
      "/selenium/profile-changelog/should-update-changelog-after-first-analysis.html").build());
  }

  private void firstAnalysis() {
    build("profile_a", "2011-06-01"); // mvn sonar:sonar -Dsonar.profile=profile_a -Dsonar.projectDate=2011-06-01
    assertThat(getProject().getMeasure("profile").getData(), is("profile_a"));
    assertThat(getProject().getMeasureValue("profile_version"), is(1.0));

    List<Event> events = getProfileEvents();
    assertThat(events.size(), is(0));
  }

  private void analyseWithNewProfile() {
    build("profile_b", "2011-06-02"); // mvn sonar:sonar -Dsonar.profile=profile_b -Dsonar.projectDate=2011-06-02
    assertThat(getProject().getMeasure("profile").getData(), is("profile_b"));
    assertThat(getProject().getMeasureValue("profile_version"), is(1.0));

    List<Event> events = getProfileEvents();
    assertThat(events.size(), is(1));
    assertEvent(events, "profile_b version 1", "profile_b version 1 is used instead of profile_a version 1");
  }

  private void analyseWithModifiedProfile() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(SELENIUM_CATEGORY,
      "/selenium/profile-changelog/modify-profile.html",
      "/selenium/profile-changelog/verify-changelog.html",
      "/selenium/profile-changelog/verify-dashboard.html").build();
    orchestrator.executeSelenese(selenese);

    build("profile_b", "2011-06-03"); // mvn sonar:sonar -Dsonar.profile=profile_b -Dsonar.projectDate=2011-06-03
    List<Event> events = getProfileEvents();
    assertThat(events.size(), is(2));
    assertEvent(events, "profile_b version 1", "profile_b version 1 is used instead of profile_a version 1");
    assertEvent(events, "profile_b version 2", "profile_b version 2 is used instead of profile_b version 1");
  }

  /**
   * SONAR-2887
   */
  private void examineChangelog() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(SELENIUM_CATEGORY,
      "/selenium/profile-changelog/examine-changelog.html").build();
    orchestrator.executeSelenese(selenese);
  }

  private void assertEvent(List<Event> events, String name, String description) {
    for (Event event : events) {
      if (name.equals(event.getName()) && description.equals(description)) {
        return;
      }
    }
    fail("Event not found");
  }

  private void build(String profile, String projectDate) {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("profile/profile-changelog"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.profile.java", profile)
      .setProperty("sonar.projectDate", projectDate);
    orchestrator.executeBuild(build);
  }

  private Resource getProject() {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "profile", "profile_version"));
  }

  private List<Event> getProfileEvents() {
    return orchestrator.getServer().getWsClient().findAll(new EventQuery().setResourceKey(PROJECT_KEY).setCategories(new String[] {"Profile"}));
  }

  private static final String PROJECT_KEY = "com.sonarsource.it.projects.profile:profile-changelog";

}
