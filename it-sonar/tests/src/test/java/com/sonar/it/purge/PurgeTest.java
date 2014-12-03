/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.purge;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PurgeTest {

  @Rule
  public ErrorCollector collector = new ErrorCollector();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-2.7.xml"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-checkstyle-plugin", "2.1.1"))
    .addPlugin(MavenLocation.of("org.codehaus.sonar-plugins.java", "sonar-pmd-plugin", "2.1"))
    .build();

  @Before
  public void deleteProjectData() {
    orchestrator.resetData();

    orchestrator.getServer().getAdminWsClient().delete(
      new PropertyDeleteQuery("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay"));
  }

  @Test
  public void test_evolution_of_number_of_rows_when_scanning_two_times_the_same_project() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    scan("shared/struts-1.3.9-diet", DateFormatUtils.ISO_DATE_FORMAT.format(yesterday));

    // count components
    collector.checkThat("Wrong number of projects", count("projects where qualifier in ('TRK','BRC')"), equalTo(4));
    collector.checkThat("Wrong number of directories", count("projects where qualifier in ('DIR')"), equalTo(40));
    collector.checkThat("Wrong number of files", count("projects where qualifier in ('FIL')"), equalTo(320));
    collector.checkThat("Wrong number of unit test files", count("projects where qualifier in ('UTS')"), equalTo(28));

    int measuresOnTrk = 182;
    int measuresOnBrc = 413;
    int measuresOnDir = 2491;
    int measuresOnFil = 10666;

    // count measures 
    logMeasures("First analysis - TRK measures", "TRK");
    logMeasures("First analysis - BRC measures", "BRC");
    assertMeasuresCountForQualifier("TRK", measuresOnTrk);
    assertMeasuresCountForQualifier("BRC", measuresOnBrc);
    assertMeasuresCountForQualifier("DIR", measuresOnDir);
    assertMeasuresCountForQualifier("FIL", measuresOnFil);

    // No new_* metrics measure should be recorded the first time
    collector.checkThat(
      "Wrong number of measure of new_ metrics",
      count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'"),
      equalTo(0));

    int expectedMeasures = measuresOnTrk + measuresOnBrc + measuresOnDir + measuresOnFil;
    collector.checkThat("Wrong number of measures", count("project_measures"), equalTo(expectedMeasures));
    collector.checkThat("Wrong number of measure data", count("project_measures where measure_data is not null"), equalTo(56));

    // count other tables that are constant between 2 scans
    int expectedIssues = 3859;
    int expectedDependencies = 977;

    collector.checkThat("Wrong number of issues", count("issues"), equalTo(expectedIssues));
    collector.checkThat("Wrong number of dependencies", count("dependencies"), equalTo(expectedDependencies));

    // must be a different date, else a single snapshot is kept per day
    scan("shared/struts-1.3.9-diet", DateFormatUtils.ISO_DATE_FORMAT.format(today));

    int newMeasuresOnTrk = 130;
    int newMeasuresOnBrc = 321;
    int newMeasuresOnDir = 577;
    int newMeasuresOnFil = 0;

    logMeasures("Second analysis - TRK measures", "TRK");
    logMeasures("Second analysis - BRC measures", "BRC");
    assertMeasuresCountForQualifier("TRK", measuresOnTrk + newMeasuresOnTrk);
    assertMeasuresCountForQualifier("BRC", measuresOnBrc + newMeasuresOnBrc);
    assertMeasuresCountForQualifier("DIR", measuresOnDir + newMeasuresOnDir);
    assertMeasuresCountForQualifier("FIL", measuresOnFil + newMeasuresOnFil);

    // Measures on new_* metrics should be recorded
    collector.checkThat(
      "Wrong number of measure of new_ metrics",
      count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'"),
      equalTo(769));

    // added measures relate to project and new_* metrics
    expectedMeasures += newMeasuresOnTrk + newMeasuresOnBrc + newMeasuresOnDir + newMeasuresOnFil;
    collector.checkThat("Wrong number of measures after second analysis", count("project_measures"), equalTo(expectedMeasures));
    collector.checkThat("Wrong number of measure data", count("project_measures where measure_data is not null"), equalTo(56));
    collector.checkThat("Wrong number of issues", count("issues"), equalTo(expectedIssues));
    collector.checkThat("Wrong number of dependencies", count("dependencies"), equalTo(expectedDependencies));
  }

  /**
   * SONAR-3378
   */
  @Test
  public void should_keep_all_snapshots_the_first_day() {

    // analyse once
    scan("shared/sample");
    // analyse twice
    scan("shared/sample");
    // and check we have 2 snapshots
    assertThat(count("snapshots s where s.project_id=(select p.id from projects p where p.kee='com.sonarsource.it.samples:simple-sample')")).isEqualTo(2);
  }

  /**
   * SONAR-2807 & SONAR-3378 & SONAR-4710
   */
  @Test
  public void should_keep_only_one_snapshot_per_day() {

    scan("shared/struts-1.3.9-diet");

    int snapshotsCount = count("snapshots where qualifier<>'LIB'");
    int measuresCount = count("project_measures");
    // Using the "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay" property set to '0' is the way
    // to keep only 1 snapshot per day
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "0"));
    scan("shared/struts-1.3.9-diet");
    assertThat(count("snapshots where qualifier<>'LIB'")).as("Different number of snapshots").isEqualTo(snapshotsCount);

    int measureOnNewMetrics = count("project_measures, metrics where metrics.id = project_measures.metric_id and metrics.name like 'new_%'");
    // Number of measures should be the same as previous, with the measures on new metrics
    assertThat(count("project_measures")).as("Different number of measures").isEqualTo(measuresCount + measureOnNewMetrics);
  }

  /**
   * SONAR-3120
   */
  @Test
  public void should_delete_removed_modules() {

    scan("purge/modules/before");
    assertSingleSnapshot("com.sonarsource.it.samples.purge:module_b");
    assertSingleSnapshot("com.sonarsource.it.samples.purge:module_b1");

    // we want the previous snapshot to be purged
    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay", "0"));
    scan("purge/modules/after");
    assertDeleted("com.sonarsource.it.samples.purge:module_b");
    assertDeleted("com.sonarsource.it.samples.purge:module_b1");
    assertSingleSnapshot("com.sonarsource.it.samples.purge:module_c");
  }

  /**
   * SONAR-3120
   */
  @Test
  public void should_delete_removed_files() {

    scan("purge/files/before");
    assertSingleSnapshot("com.sonarsource.it.samples.purge:files:src/main/java/sample/Sample.java");
    assertSingleSnapshot("com.sonarsource.it.samples.purge:files:src/main/java/sample/Sample.java");

    scan("purge/files/after");
    assertDeleted("com.sonarsource.it.samples.purge:files:src/main/java/sample/Sample.java");
    assertDeleted("com.sonarsource.it.samples.purge:files:src/main/java/sample/Sample.java");
    assertSingleSnapshot("com.sonarsource.it.samples.purge:files:src/main/java/sample/NewSample.java");
  }

  /**
   * SONAR-2754
   */
  @Test
  public void should_delete_historical_data_of_directories_by_default() {

    scan("shared/sample", "2012-01-01");
    String select = "snapshots where scope='DIR'";
    int directorySnapshots = count(select);

    scan("shared/sample", "2012-02-02");
    assertThat(count(select)).isEqualTo(directorySnapshots);
  }

  /**
   * SONAR-2754
   */
  @Test
  public void should_not_delete_historical_data_of_directories() {
    scan("shared/sample", "2012-01-01");

    String select = "snapshots where scope='DIR'";
    int directorySnapshots = count(select);

    orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.dbcleaner.cleanDirectory", "false"));
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("shared/sample"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProfile("sonar-way-2.7")
      .setProperty("sonar.projectDate", "2012-02-02");

    orchestrator.executeBuild(build);

    assertThat(count(select)).isEqualTo(2 * directorySnapshots);
  }

  /**
   * SONAR-2061
   */
  @Test
  public void should_delete_historical_data_of_flagged_metrics() {

    scan("shared/sample", "2012-01-01");

    // historical data of complexity_in_classes is supposed to be deleted (see CoreMetrics)
    String selectNcloc = "project_measures where metric_id in (select id from metrics where name='ncloc')";
    String selectComplexityInClasses = "project_measures where metric_id in (select id from metrics where name='complexity_in_classes')";
    int nclocCount = count(selectNcloc);
    int complexitInClassesCount = count(selectComplexityInClasses);

    scan("shared/sample", "2012-02-02");
    assertThat(count(selectNcloc)).isGreaterThan(nclocCount);
    assertThat(count(selectComplexityInClasses)).isEqualTo(complexitInClassesCount);
  }

  private void assertDeleted(String key) {
    assertThat(count("snapshots s where s.project_id=(select p.id from projects p where p.kee='" + key + "')")).isZero();
    assertThat(count("resource_index ri where ri.resource_id=(select p.id from projects p where p.kee='" + key + "')")).isZero();
  }

  private void assertSingleSnapshot(String key) {
    assertThat(count("snapshots s where s.project_id=(select p.id from projects p where p.kee='" + key + "')")).isEqualTo(1);
    assertThat(count("resource_index ri where ri.resource_id=(select p.id from projects p where p.kee='" + key + "')")).isGreaterThan(1);
  }

  private MavenBuild scan(String path, String date) {
    return scan(path, "sonar.projectDate", date);
  }

  private MavenBuild scan(String path, String... extraProperties) {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom(path))
      .setGoals("clean package", "sonar:sonar")
      .setProperty("skipTests", "true")
      .setProfile("sonar-way-2.7");
    if (extraProperties != null) {
      build.setProperties(extraProperties);
    }
    orchestrator.executeBuild(build);
    return build;
  }

  private int count(String condition) {
    return orchestrator.getDatabase().countSql("select count(*) from " + condition);
  }

  private void assertMeasuresCountForQualifier(String qualifier, int count) {
    int result = countMeasures(qualifier);
    logMeasures("GOT", qualifier);
    collector.checkThat("Wrong number of measures for qualifier " + qualifier, result, equalTo(count));
  }

  private int countMeasures(String qualifier) {
    String sql = "SELECT count(pm.id) FROM project_measures pm, snapshots s, metrics m where pm.snapshot_id=s.id and pm.metric_id=m.id and s.qualifier='" + qualifier + "'";
    return orchestrator.getDatabase().countSql(sql);
  }

  private void logMeasures(String title, String qualifier) {
    String sql = "SELECT m.name as metricName, pm.value as value, pm.text_value as textValue, pm.variation_value_1, pm.variation_value_2, pm.variation_value_3, pm.rule_id, pm.characteristic_id "
      +
      "FROM project_measures pm, snapshots s, metrics m " +
      "WHERE pm.snapshot_id=s.id and pm.metric_id=m.id and s.qualifier='"
      + qualifier + "'";
    List<Map<String, String>> rows = orchestrator.getDatabase().executeSql(sql);

    System.out.println("---- " + title + " - measures on qualifier " + qualifier);
    for (Map<String, String> row : rows) {
      System.out.println("  " + row);
    }
  }

}
