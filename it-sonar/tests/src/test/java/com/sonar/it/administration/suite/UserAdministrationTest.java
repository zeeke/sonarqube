/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.administration.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.user.User;
import org.sonar.wsclient.user.UserParameters;
import org.sonar.wsclient.user.UserQuery;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class UserAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = AdministrationTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/sonar-way-2.7.xml"));
  }

  /**
   * SONAR-3667
   * SONAR-3788
   */
  @Test
  public void should_change_user_password() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-administration-change-password",
      "/selenium/administration/user-administration/change-password.html",
      "/selenium/administration/user-administration/change-password-with-existing-deactivate-user.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void create_user() throws IOException {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-administration-create_user",
      "/selenium/administration/user-administration/create-user.html").build();
    orchestrator.executeSelenese(selenese);

    File logs = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logs)).contains("NEW USER - login=simon, name=Simon");
  }

  @Test
  public void delete_and_reactivate_user() throws IOException {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-administration-delete_and_reactivate_user",
      "/selenium/administration/user-administration/reactivate-user.html").build();
    orchestrator.executeSelenese(selenese);

    File logs = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logs)).contains("NEW USER - login=reactivated, name=Reactivated");
  }

  @Test
  public void allow_users_to_sign_up() throws IOException {
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.allowUsersToSignUp", "true"));

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-administration-allow_users_to_sign_up",
      "/selenium/administration/user-administration/allow_users_to_sign_up.html").build();
    orchestrator.executeSelenese(selenese);

    File logs = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logs)).contains("NEW USER - login=signuplogin, name=SignUpName");

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.allowUsersToSignUp", "false"));
  }

  /**
   * SONAR-4323
   */
  @Test
  public void search_for_all_users() throws Exception {
    // no need for admin rights
    SonarClient client = ItUtils.newWsClientForAnonymous(orchestrator);

    List<User> users = client.userClient().find(UserQuery.create());

    assertThat(users).isNotEmpty();
    for (User user : users) {
      assertThat(user.login()).isNotEmpty();
      assertThat(user.name()).isNotEmpty();
    }
  }

  /**
   * SONAR-4323
   */
  @Test
  public void search_for_user_by_login() throws Exception {
    // no need for admin rights
    SonarClient client = ItUtils.newWsClientForAnonymous(orchestrator);
    List<User> users = client.userClient().find(UserQuery.create().logins("admin"));

    assertThat(users).hasSize(1);
    assertThat(users.get(0).name()).isEqualTo("Administrator");
  }

  /**
   * SONAR-4323
   */
  @Test
  public void search_for_user_by_text() throws Exception {
    // no need for admin rights
    SonarClient client = ItUtils.newWsClientForAnonymous(orchestrator);
    List<User> users = client.userClient().find(UserQuery.create().searchText("adm"));

    assertThat(users).hasSize(1);
    assertThat(users.get(0).name()).isEqualTo("Administrator");

    assertThat(client.userClient().find(UserQuery.create().searchText("xxxx"))).isEmpty();
  }

  /**
   * SONAR-4411
   */
  @Test
  public void should_create_user_using_ws() throws Exception {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters creationParameters = UserParameters.create().login("should_create_user").password("password").passwordConfirmation("password");
    client.userClient().create(creationParameters);

    UserParameters reactivationParameters = UserParameters.create().login("should_reactivate_user").password("password").passwordConfirmation("password");
    client.userClient().create(reactivationParameters);
    client.userClient().deactivate("should_reactivate_user");
    client.userClient().create(reactivationParameters);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-creation-using-ws",
      "/selenium/administration/user-administration/create-user-with-ws.html",
      "/selenium/administration/user-administration/reactivate-user-with-ws.html")
      .build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4411
   */
  @Test
  public void should_edit_user_using_ws() throws Exception {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters userCreationParameters = UserParameters.create().login("should_edit_user").password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);

    UserParameters userEditionParameters = UserParameters.create().login("should_edit_user").email("should_edit_user@mail.net");
    client.userClient().update(userEditionParameters);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-edition-using-ws",
      "/selenium/administration/user-administration/edit-user-with-ws.html").build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4411
   */
  @Test
  public void should_delete_user_using_ws() throws Exception {
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);

    UserParameters creationParameters = UserParameters.create().login("should_delete_user").password("password").passwordConfirmation("password");
    client.userClient().create(creationParameters);
    client.userClient().deactivate("should_delete_user");

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("user-deletion-using-ws",
      "/selenium/administration/user-administration/delete-user-with-ws.html").build();
    orchestrator.executeSelenese(selenese);
  }
}
