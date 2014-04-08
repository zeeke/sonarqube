/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.user;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.After;
import org.junit.Test;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.connectors.HttpClient4Connector;
import org.sonar.wsclient.services.AuthenticationQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.UserPropertyCreateQuery;
import org.sonar.wsclient.services.UserPropertyQuery;
import org.sonar.wsclient.user.UserParameters;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ExternalSecurityTest {

  private Orchestrator orchestrator = null;

  /**
   * Property from security-plugin for user management.
   */
  private static final String USERS_PROPERTY = "sonar.fakeauthenticator.users";

  private void start(Map<String, String> securityProperties) {
    OrchestratorBuilder builder = Orchestrator.builderEnv().addPlugin(ItUtils.locateTestPlugin("security-plugin"));
    for (Map.Entry<String, String> entry : securityProperties.entrySet()) {
      builder.setServerProperty(entry.getKey(), entry.getValue());
    }

    orchestrator = builder.build();
    orchestrator.start();
  }

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  /**
   * SONAR-3137, SONAR-2292
   * Restriction on password length (minimum 4 characters) should be disabled, when external system enabled.
   */
  @Test
  public void shouldSynchronizeDetailsAndGroups() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm"));
    String username = "tester";
    String password = "123";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(username + ".password", password);
    users.put(username + ".name", "Tester Testerovich");
    users.put(username + ".email", "tester@example.org");
    users.put(username + ".groups", "sonar-user");
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(username, password), is(AUTHORIZED));
    // with external details and groups
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("external-user-details",
      "/selenium/user/external-security/external-user-details.html").build());

    // SONAR-4462
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("system-info",
      "/selenium/user/external-security/system-info.html").build());
  }

  /**
   * SONAR-4034
   */
  @Test
  public void shouldUpdateDetailsByDefault() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm"));
    String username = "tester";
    String password = "123";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(username + ".password", password);
    users.put(username + ".name", "Tester Testerovich");
    users.put(username + ".email", "tester@example.org");
    users.put(username + ".groups", "sonar-user");
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(username, password), is(AUTHORIZED));
    // with external details and groups
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("external-user-details",
      "/selenium/user/external-security/external-user-details.html").build());

    // Now update user details
    users.put(username + ".name", "Tester2 Testerovich");
    users.put(username + ".email", "tester2@example.org");
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(username, password), is(AUTHORIZED));
    // with external details and groups updated
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("external-user-details2",
      "/selenium/user/external-security/external-user-details2.html").build());
  }

  /**
   * SONAR-4034
   */
  @Test
  public void shouldNotUpdateDetailsIfRequested() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm", "sonar.security.updateUserAttributes", "false"));
    String username = "tester";
    String password = "123";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(username + ".password", password);
    users.put(username + ".name", "Tester Testerovich");
    users.put(username + ".email", "tester@example.org");
    users.put(username + ".groups", "sonar-user");
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(username, password), is(AUTHORIZED));
    // with external details and groups
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("external-user-details",
      "/selenium/user/external-security/external-user-details.html").build());

    // Now update user details
    users.put(username + ".name", "Tester2 Testerovich");
    users.put(username + ".email", "tester2@example.org");
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(username, password), is(AUTHORIZED));
    // with external details and groups not updated
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("external-user-details",
      "/selenium/user/external-security/external-user-details.html").build());
  }

  /**
   * SONAR-3138
   */
  @Test
  public void shouldFallback() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm", "sonar.security.savePassword", "true"));
    String login = "tester";
    String oldPassword = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(login + ".password", oldPassword);
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(login, oldPassword), is(AUTHORIZED));

    // When new external password was set
    String newPassword = "7654321";
    users.put(login + ".password", newPassword);
    updateUsers(users);
    // Then
    assertThat("New password works",
      loginAttempt(login, newPassword), is(AUTHORIZED));
    assertThat("Old password does not work",
      loginAttempt(login, oldPassword), is(NOT_AUTHORIZED));
    assertThat("Wrong password does not work",
      loginAttempt(login, "wrong"), is(NOT_AUTHORIZED));

    // When external system does not work
    users.remove(login + ".password");
    updateUsers(users);
    // Then
    assertThat("New password works (fallback)",
      loginAttempt(login, newPassword), is(AUTHORIZED));
    assertThat("Old password does not work",
      loginAttempt(login, oldPassword), is(NOT_AUTHORIZED));
    assertThat("Wrong password does not work",
      loginAttempt(login, "wrong"), is(NOT_AUTHORIZED));
  }

  /**
   * SONAR-3138
   */
  @Test
  public void shouldNotFallback() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm"));
    String login = "tester";
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(login + ".password", password);
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(login, password), is(AUTHORIZED));

    // When external system does not work
    users.remove(login + ".password");
    updateUsers(users);
    // Then
    assertThat("User can't login",
      loginAttempt(login, password), is(NOT_AUTHORIZED));
  }

  /**
   * SONAR-4543
   */
  @Test
  public void shouldNotAccessExternalSystemForLocalAccounts() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm", "sonar.security.savePassword", "false"));
    String login = "localuser";
    String localPassword = "1234567";
    String remotePassword = "7654321";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(login + ".password", remotePassword);
    updateUsers(users);
    // And user exists in local database
    orchestrator.getServer().adminWsClient().userClient().create(UserParameters.create().login(login).name(login).password(localPassword).passwordConfirmation(localPassword));

    // Then this is external system that should be used
    assertThat("User authenticated by external system",
      loginAttempt(login, remotePassword), is(AUTHORIZED));
    assertThat("User not authenticated by local DB",
      loginAttempt(login, localPassword), is(NOT_AUTHORIZED));

    // Now set this user as technical account
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.security.localUsers", "admin," + login));

    // Then this is local DB that should be used
    assertThat("User not authenticated by external system",
      loginAttempt(login, remotePassword), is(NOT_AUTHORIZED));
    assertThat("User authenticated by local DB",
      loginAttempt(login, localPassword), is(AUTHORIZED));
  }

  /**
   * SONAR-4543
   */
  @Test
  public void adminIsLocalAccountByDefault() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm", "sonar.security.savePassword", "false"));
    String login = "admin";
    String localPassword = "admin";
    String remotePassword = "nimda";
    Map<String, String> users = Maps.newHashMap();

    // When admin created in external system with a different password
    users.put(login + ".password", remotePassword);
    updateUsers(users);

    // Then this is local DB that should be used
    assertThat("Admin not authenticated by external system",
      loginAttempt(login, remotePassword), is(NOT_AUTHORIZED));
    assertThat("Admin authenticated by local DB",
      loginAttempt(login, localPassword), is(AUTHORIZED));
  }

  /**
   * SONAR-1334, SONAR-3185 (createUsers=true is default)
   */
  @Test
  public void shouldCreateNewUsers() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm"));
    String username = "tester";
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user not exists in external system
    // Then
    assertThat("User not created in Sonar",
      loginAttempt(username, password), is(NOT_AUTHORIZED));

    // When user created in external system
    users.put(username + ".password", password);
    updateUsers(users);
    // Then
    assertThat("User created in Sonar",
      loginAttempt(username, password), is(AUTHORIZED));
    assertThat("Wrong password does not work",
      loginAttempt(username, "wrong"), is(NOT_AUTHORIZED));
  }

  /**
   * SONAR-1334 (createUsers=false)
   */
  @Test
  public void shouldNotCreateNewUsers() {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm", "sonar.authenticator.createUsers", "false"));
    String username = "tester";
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user not exists in external system
    // Then
    assertThat("User not created in Sonar",
      loginAttempt(username, password), is(NOT_AUTHORIZED));

    // When user created in external system
    users.put(username + ".password", password);
    updateUsers(users);
    // Then
    assertThat("User not created in Sonar",
      loginAttempt(username, password), is(NOT_AUTHORIZED));
  }

  // SONAR-3258
  @Test
  public void shouldAutomaticallyReactivateDeletedUser() throws Exception {
    // Given clean Sonar installation and no users in external system
    start(ImmutableMap.of("sonar.security.realm", "FakeRealm"));

    // Let's create and delete the user "tester" in Sonar DB
    orchestrator.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("external-user-create-and-delete-user",
      "/selenium/user/external-security/create-and-delete-user.html").build());

    // And now update the security with the user that was deleted
    String login = "tester";
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();
    users.put(login + ".password", password);
    updateUsers(users);
    // check that the deleted/deactivated user "tester" has been reactivated and can now log in
    assertThat("User created in Sonar",
      loginAttempt(login, password), is(AUTHORIZED));
  }

  @Test
  public void shouldTestAuthenticationWithWebService() {
    orchestrator = Orchestrator.builderEnv().build();
    orchestrator.start();

    assertThat(checkAuthenticationThroughWebService("admin", "admin")).isTrue();
    assertThat(checkAuthenticationThroughWebService("wrong", "admin")).isFalse();
    assertThat(checkAuthenticationThroughWebService("admin", "wrong")).isFalse();
    assertThat(checkAuthenticationThroughWebService(null, null)).isTrue();

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

    assertThat(checkAuthenticationThroughWebService("admin", "admin")).isTrue();
    assertThat(checkAuthenticationThroughWebService("wrong", "admin")).isFalse();
    assertThat(checkAuthenticationThroughWebService("admin", "wrong")).isFalse();
    assertThat(checkAuthenticationThroughWebService(null, null)).isFalse();
  }

  private boolean checkAuthenticationThroughWebService(String login, String password) {
    return createWsClient(login, password).find(new AuthenticationQuery()).isValid();
  }

  private static String AUTHORIZED = "authorized";
  private static String NOT_AUTHORIZED = "not authorized";

  /**
   * Utility method to check that user can be authorized.
   *
   * @throws IllegalStateException
   */
  private String loginAttempt(String username, String password) {
    String expectedValue = Long.toString(System.currentTimeMillis());
    Sonar wsClient = createWsClient(username, password);
    try {
      wsClient.create(new UserPropertyCreateQuery("auth", expectedValue));
    } catch (ConnectionException e) {
      return NOT_AUTHORIZED;
    }
    try {
      String value = wsClient.find(new UserPropertyQuery("auth")).getValue();
      if (!Objects.equal(value, expectedValue)) {
        // exceptional case - update+retrieval were successful, but value doesn't match
        throw new IllegalStateException("Expected " + expectedValue + " , but got " + value);
      }
    } catch (ConnectionException e) {
      // exceptional case - update was successful, but not retrieval
      throw new IllegalStateException(e);
    }
    return AUTHORIZED;
  }

  /**
   * Updates information about users in security-plugin.
   */
  private void updateUsers(Map<String, String> users) {
    try {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery(USERS_PROPERTY, format(users)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to update configuration of plugin", e);
    }
  }

  private static String format(Map<String, String> map) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }
    return sb.toString();
  }

  /**
   * Utility method to create {@link org.sonar.wsclient.Sonar} with specified {@code username} and {@code password}.
   * Orchestrator does not provide such method.
   */
  private Sonar createWsClient(String username, String password) {
    return new Sonar(new HttpClient4Connector(new Host(orchestrator.getServer().getUrl(), username, password)));
  }

}
