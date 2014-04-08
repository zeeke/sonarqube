/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.profile;

import com.google.common.collect.ImmutableMap;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.locator.MavenLocation;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Profile;
import org.sonar.wsclient.services.ProfileQuery;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ProfileLoadingTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Orchestrator orchestrator;

  @After
  public void stopServer() throws Exception {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  private void startServer(Map<String, String> extraProperties) {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    builder.addPlugin(ItUtils.locateTestPlugin("several-profiles-plugin"));
    for (Map.Entry<String, String> entry : extraProperties.entrySet()) {
      builder.setServerProperty(entry.getKey(), entry.getValue());
    }
    orchestrator = builder.build();
    orchestrator.start();
  }

  @Test
  public void shouldGetSonarWayAsDefaultProfileForLanguageJava() {
    startServer(Collections.<String, String>emptyMap());

    Sonar wsClient = orchestrator.getServer().getAdminWsClient();
    Profile defaultProfile = wsClient.find(ProfileQuery.createWithLanguage("java"));
    assertThat(defaultProfile.getName(), is("Sonar way"));
    assertThat(defaultProfile.isDefaultProfile(), is(true));
  }

  // SONAR-2977
  @Test
  public void shouldLoadCorrectDefaultProfileForLanguageFoo() {
    // Given that:
    // "Profile1" is a standard profile
    // "Profile2" is set as default profile
    // "Profile3" (ModifiableProfile) is a standard profile
    startServer(Collections.<String, String>emptyMap());

    // Check that Profile2 is the default profile
    Sonar wsClient = orchestrator.getServer().getAdminWsClient();
    Profile defaultProfile = wsClient.find(ProfileQuery.createWithLanguage("foo"));
    assertThat(defaultProfile.getName(), is("Profile2"));
    assertThat(defaultProfile.isDefaultProfile(), is(true));
  }

  // SONAR-2977
  @Test
  public void shouldFailSonarStartupIfMoreThanOneProvidedProfileByLanguage() {
    thrown.expect(IllegalStateException.class);

    // Given that:
    // "Profile1" is a standard profile
    // "Profile2" is set as default profile
    // "Profile3" (ModifiableProfile) is also set as default profile
    startServer(ImmutableMap.of("sonar.modifiable_profile.default", "true"));
  }
}
