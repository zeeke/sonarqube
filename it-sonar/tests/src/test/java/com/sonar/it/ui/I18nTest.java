/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.ui;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleQuery;
import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.ViolationQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class I18nTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("l10n-fr-pack"))
      .addPlugin(ItUtils.locateTestPlugin("self-l10ned-plugin"))
      .build();

  @After
  public void cleanDatabase() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  @Test
  public void test_localization() {
    MavenBuild build = MavenBuild.builder()
        .setPom(ItUtils.locateProjectPom("shared/sample"))
        .addSonarGoal()
        .withDynamicAnalysis(false)
        .build();
    orchestrator.executeBuild(build);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("ui-i18n",
        "/selenium/ui/i18n/default-locale-is-english.html",
        "/selenium/ui/i18n/french-locale.html",
        "/selenium/ui/i18n/french-pack.html",
        "/selenium/ui/i18n/french-rules.html",
        "/selenium/ui/i18n/locale-with-france-country.html",
        "/selenium/ui/i18n/locale-with-swiss-country.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_search_rule_by_name_localized_in_pack() {
    RuleQuery ruleQuery = (RuleQuery) new RuleQuery("java")
        .setSearchText("Contrainte d'architecture")
        .setLocale("fr");
    Rule rule = orchestrator.getServer().getWsClient().find(ruleQuery);
    assertThat(rule).isNotNull();
    assertThat(rule.getDescription()).contains("Un code source est conforme à un modèle architectural");
  }

  @Test
  public void should_search_rule_by_name_localized_in_same_plugin() {
    RuleQuery ruleQuery = (RuleQuery) new RuleQuery("java")
        .setSearchText("Ma regle")
        .setLocale("fr");
    Rule rule = orchestrator.getServer().getWsClient().find(ruleQuery);
    assertThat(rule).isNotNull();
    assertThat(rule.getRepository()).isEqualTo("myrepo");
    assertThat(rule.getDescription()).contains("Description HTML de la regle myrule");
  }

  @Test
  public void shouldSearchLocalizedNameOnlyWithinUserLocale() {
    RuleQuery ruleQuery = (RuleQuery) new RuleQuery("java")
        .setSearchText("Contrainte d'architecture")
        .setLocale("en-gb");
    Rule rule = orchestrator.getServer().getWsClient().find(ruleQuery);
    assertThat(rule).isNull();
  }

  @Test
  public void plugins_could_provide_their_own_bundles() {
    MavenBuild build = MavenBuild.builder()
        .setPom(ItUtils.locateProjectPom("shared/sample"))
        .addSonarGoal()
        .withDynamicAnalysis(false)
        .build();
    String logs = orchestrator.executeBuild(build).getLogs();

    assertThat(logs).contains("> Ceci est un message");
    assertThat(logs).contains("> Description HTML de la regle myrule");
    assertThat(logs).contains("> Ma regle");
  }

  /**
   * SONAR-3319
   */
  @Test
  public void shouldUseNewDescriptionFileLocationWhenAvailable() {
    RuleQuery ruleQuery = (RuleQuery) new RuleQuery("java")
        .setSearchText("Fichier vide")
        .setLocale("fr");
    Rule rule = orchestrator.getServer().getWsClient().find(ruleQuery);
    assertThat(rule).isNotNull();
    // the "EmptyFile.html" file located in "org.sonar.l10n.squijava_fr.rules.squid" must supersede the one located in
    // "org.sonar.l10n.squijava_fr"
    assertThat(rule.getDescription()).contains("Comment: NEW LOCATION for HTML description files (since Sonar 2.15).");
  }

}
