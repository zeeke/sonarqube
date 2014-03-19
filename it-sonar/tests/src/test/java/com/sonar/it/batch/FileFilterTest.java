/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.locator.MavenLocation;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * The Cut-Off plugin is the first plugin which implements the extension point FileFilter.
 */
public class FileFilterTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(MavenLocation.create("org.codehaus.sonar-plugins", "sonar-cutoff-plugin", "0.1.1"))
    .build();

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldExcludeFilesModifiedBeforeCutoffDate() throws Exception {
    File projectDir = tempFolder.newFolder();
    FileUtils.copyDirectory(ItUtils.locateProjectDir("batch/file-filter"), projectDir);

    File oldFile = new File(projectDir, "src/main/java/Old.java");
    oldFile.setLastModified(new SimpleDateFormat("yyyy-MM-dd").parse("2007-05-18").getTime());

    MavenBuild build = MavenBuild.create(new File(projectDir, "pom.xml"))
      .setCleanSonarGoals()
      .setProperty("sonar.cutoff.date", "2008-12-25");
    orchestrator.executeBuild(build);

    List<Resource> files = orchestrator.getServer().getWsClient().findAll(
      ResourceQuery.createForMetrics("com.sonarsource.it.projects.batch:file-filter", "lines")
        .setAllDepths()
        .setScopes("FIL"));
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getName()).isEqualTo("Young.java");
  }
}
