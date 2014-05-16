/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.rule;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.fest.assertions.Condition;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleParam;
import org.sonar.wsclient.services.RuleQuery;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("New rule templates are under development")
public class RulesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("beta-rule-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("deprecated-rule-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin", "2.1"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1"))
    .build();

  @Test
  public void test_rule_template() {
    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("rule-template",
        "/selenium/rule/rules/copy_and_edit_rule_template.html",
        "/selenium/rule/rules/copy_and_delete_rule_template.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_search_rules() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/rule/RulesTest/Sonar_way_java-profile.xml"));

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("search-rules",
        "/selenium/rule/search-rules/rule_search.html",
        // SONAR-3936
        "/selenium/rule/search-rules/rule_search_verify_form_values_on_first_call.html",
        // SONAR-3936
        "/selenium/rule/search-rules/search_and_display_inactive_rules.html",
        // SONAR-3966
        "/selenium/rule/search-rules/search_by_plugin.html",
        "/selenium/rule/search-rules/search_by_rule_priority.html",
        "/selenium/rule/search-rules/search_by_rule_activation.html",
        "/selenium/rule/search-rules/search_by_rule_title.html",
        // SONAR-3879
        "/selenium/rule/search-rules/search_by_status.html",
        "/selenium/rule/search-rules/expand_and_collapse.html",
        // SONAR-4193
        "/selenium/rule/search-rules/display-link-to-another-rule-in-description-rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  @Ignore("New rule templates are under development")
  public void should_edit_rules() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/rule/RulesTest/rule-with-parameters-profile.xml"));
    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("edit-rules",
        "/selenium/rule/edit_rules/edit-string.html",
        "/selenium/rule/edit_rules/edit-text.html", // SONAR-1995
        "/selenium/rule/edit_rules/edit-integer.html", // SONAR-3432
        "/selenium/rule/edit_rules/edit-float.html",
        "/selenium/rule/edit_rules/edit-boolean.html", // SONAR-4568
        "/selenium/rule/edit_rules/update-parameter-twice-to-null-value.html" // SONAR-4568
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-2958
   * SONAR-5023
   */
  @Test
  public void ws_description_field_should_not_be_null() {
    RuleQuery query = new RuleQuery("java").setRepositories("checkstyle").setSearchText("com.puppycrawl.tools.checkstyle.checks.naming.AbstractClassNameCheck");
    Rule rule = orchestrator.getServer().getWsClient().find(query);
    assertThat(rule.getDescription().length()).isGreaterThan(10);

    assertThat(rule.getParams().size()).isGreaterThanOrEqualTo(1);
    // the parameter "format" has no description
    assertThat(rule.getParams()).satisfies(new ContainsParamCondition("format", null));

    // SONAR-5023
    assertThat(rule.getParams()).satisfies(new ContainsParamCondition("ignoreModifier",
      "Controls whether to ignore checking for the abstract modifier on classes that match the name. Default is false."));
  }

  @Test
  @Ignore("New rules are under development")
  public void should_manage_tags_on_rules() {
//    final SonarClient wsClient = orchestrator.getServer().adminWsClient();
//
//    assertThat(wsClient.ruleTagClient().list()).excludes("mytag1", "mytag2", "mytag3");
//    wsClient.ruleTagClient().create("mytag1");
//    wsClient.ruleTagClient().create("mytag2");
//    wsClient.ruleTagClient().create("mytag3");
//    assertThat(wsClient.ruleTagClient().list()).contains("mytag1", "mytag2", "mytag3");
//
//    // select tags 1 and 3
//    wsClient.ruleClient().addTags("xoo:OneIssuePerLine", "mytag1", "mytag3");
//    // tag 2 should disappear (not associated to any rule)
//    assertThat(wsClient.ruleTagClient().list()).contains("mytag1", "mytag3").excludes("mytag2");
//
//    // TODO 4.3 Check that rule appears in search filtered by tags, and that tags are set on rule
//
//    // remove tags 1 and 3
//    wsClient.ruleClient().removeTags("xoo:OneIssuePerLine", "mytag1", "mytag3");
//    // no more tags
//    assertThat(wsClient.ruleTagClient().list()).excludes("mytag1", "mytag2", "mytag3");
  }

  private static class ContainsParamCondition extends Condition<List<?>> {

    private String ruleName;
    private String ruleDescription;

    private ContainsParamCondition(String ruleName, String ruleDescription) {
      this.ruleName = ruleName;
      this.ruleDescription = ruleDescription;
    }

    @Override
    public boolean matches(List<?> list) {
      for (RuleParam rp : (List<RuleParam>) list) {
        if (ruleName.equals(rp.getName()) &&
          ((ruleDescription != null && ruleDescription.equals(rp.getDescription()))
          || (ruleDescription == null && rp.getDescription() == null))) {
          return true;
        }
      }
      return false;
    }
  }
}
