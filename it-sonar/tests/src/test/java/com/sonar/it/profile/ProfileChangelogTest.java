/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Event;
import org.sonar.wsclient.services.EventQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ProfileChangelogTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv().build();

  @BeforeClass
  public static void restoreBackup() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/profile_a.xml"));
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/profile/ProfileChangelogTest/profile_b.xml"));
  }

  private static final String SELENIUM_CATEGORY = "profile-changelog";

  @Test
  public void shouldCreateChangelog() {
    firstAnalysis();
    analyseWithNewProfile();
    analyseWithModifiedProfile();
    examineChangelog();
  }

  /**
   * SONAR-2621
   */
  @Test
  public void shouldBeAvailableToAnonymous() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath(SELENIUM_CATEGORY, "/selenium/profile-changelog/anonymous-access.html").build();
    orchestrator.executeSelenese(selenese);
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
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("profile/profile-changelog"))
      .addSonarGoal()
      .withDynamicAnalysis(false)
      .withProperty("sonar.profile", profile)
      .withProperty("sonar.projectDate", projectDate)
      .build();
    orchestrator.executeBuild(build);
  }

  private Resource getProject() {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, "profile", "profile_version"));
  }

  private List<Event> getProfileEvents() {
    return orchestrator.getServer().getWsClient().findAll(new EventQuery().setResourceKey(PROJECT_KEY).setCategories(new String[]{"Profile"}));
  }

  private static final String PROJECT_KEY = "com.sonarsource.it.projects.profile:profile-changelog";

}
