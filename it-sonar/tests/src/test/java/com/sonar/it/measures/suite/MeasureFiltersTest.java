/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.measures.suite;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.selenium.Selenese;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.user.UserParameters;

public class MeasureFiltersTest {

  @ClassRule
  public static Orchestrator orchestrator = MeasuresTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void scanStruts() {
    orchestrator.getDatabase().truncateInspectionTables();
    MavenBuild build = MavenBuild.builder()
      .setPom(ItUtils.locateProjectPom("shared/struts-1.3.9-diet")).addSonarGoal()
      .withDynamicAnalysis(true)
      .build();
    orchestrator.executeBuild(build);
    createUser();
  }

  private static void createUser(){
    SonarClient client = ItUtils.newWsClientForAdmin(orchestrator);
    UserParameters userCreationParameters = UserParameters.create().login("user-measure-filters").name("User Measure Filters").password("password").passwordConfirmation("password");
    client.userClient().create(userCreationParameters);
  }


  @Test
  public void execute_measure_filters() {
    // TODO delete columns, list pagination, no results if no criteria, date criteria, period criteria, favourites, language, root
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("execution_of_measure_filters",
      "/selenium/measures/measure_filters/link_from_main_header.html",
      "/selenium/measures/measure_filters/initial_search_form.html",
      "/selenium/measures/measure_filters/search_for_projects.html",
      "/selenium/measures/measure_filters/search_for_files.html",
      "/selenium/measures/measure_filters/empty_filter.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void display_measure_filter_as_list() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("display_measure_filter_as_list",
      "/selenium/measures/measure_filters/list_change_columns.html",
      "/selenium/measures/measure_filters/list_delete_column.html",
      "/selenium/measures/measure_filters/list_move_columns.html",
      "/selenium/measures/measure_filters/list_sort_by_descending_name.html",
      "/selenium/measures/measure_filters/list_sort_by_ncloc.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void display_measure_filter_as_treemap() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("display_measure_filter_as_treemap",
      "/selenium/measures/measure_filters/treemap_display.html",
      "/selenium/measures/measure_filters/treemap_change_metrics.html",
      "/selenium/measures/measure_filters/treemap_of_components_widget.html",
      "/selenium/measures/measure_filters/treemap_of_components_widget_edit_metrics.html",
      "/selenium/measures/measure_filters/treemap_of_filter_widget.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void share_measure_filters() {
    //TODO
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("share_measure_filters",
      // SONAR-4469
      "/selenium/measures/measure_filters/should-unshare-filter-remove-other-filters-favourite.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void favourite_measure_filters() {
    // save, delete, edit, store
    //TODO
  }

  @Test
  public void copy_measure_filters() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("copy_measure_filters",
      "/selenium/measures/measure_filters/copy_measure_filter.html",
      "/selenium/measures/measure_filters/copy_uniqueness_of_name.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void manage_measure_filters() {
    // TODO save with description
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("manage_measure_filters",
      "/selenium/measures/measure_filters/save_with_special_characters.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void measure_filter_list_widget() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("measure_filter_list_widget",
      "/selenium/measures/measure_filters/list_widget.html",
      "/selenium/measures/measure_filters/list_widget_sort.html",
      "/selenium/measures/measure_filters/list_widget_warning_if_missing_filter.html"
    ).build();
    orchestrator.executeSelenese(selenese);
  }

}
