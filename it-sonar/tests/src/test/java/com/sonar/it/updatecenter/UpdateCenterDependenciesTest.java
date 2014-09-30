/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.updatecenter;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class UpdateCenterDependenciesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  File updateCenterFile;
  Orchestrator orchestrator;

  @Before
  public void before() throws Exception {
    URL updateCenter = UpdateCenterDependenciesTest.class.getResource("UpdateCenterTest/update-center-dependencies.properties");
    updateCenterFile = temp.newFile("update-center.properties");
    File file = new File(updateCenter.toURI());
    FileInputStream in = new FileInputStream(file);
    try {
      Properties props = new Properties();
      props.load(in);
      props.store(new FileOutputStream(updateCenterFile), "Update center file created.");

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void should_not_start_when_dependency_is_missing() throws IOException {
    orchestrator = Orchestrator
      .builderEnv()
      .setServerProperty("sonar.updatecenter.url",
        UpdateCenterDependenciesTest.class.getResource("UpdateCenterTest/update-center-dependencies.properties").toString())
      // C Sharp requires DotNet
      .addPlugin(ItUtils.locateTestPlugin("update-center/csharp-plugin-1.0", "csharp-plugin-v10", "1.0"))
      .build();

    try {
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(getServerLog()).contains("The plugin 'dotnet' required by 'csharp' is missing");
    }
  }

  @Test
  public void should_not_start_when_dependency_version_is_wrong() throws IOException {
    orchestrator = Orchestrator
      .builderEnv()
      .setServerProperty("sonar.updatecenter.url",
        UpdateCenterDependenciesTest.class.getResource("UpdateCenterTest/update-center-dependencies.properties").toString())
      // C Sharp 1.1 requires DotNet 1.1
      .addPlugin(ItUtils.locateTestPlugin("update-center/csharp-plugin-1.1", "csharp-plugin-v11", "1.1"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.0", "dotnet-plugin-v10", "1.0"))
      .build();

    try {
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(getServerLog()).contains("The plugin 'dotnet' is in version 1.0 whereas the plugin 'csharp' requires a least a version 1.1");
    }
  }

  @Test
  public void should_not_start_when_parent_is_missing() throws IOException {
    orchestrator = Orchestrator
      .builderEnv()
      .setServerProperty("sonar.updatecenter.url",
        UpdateCenterDependenciesTest.class.getResource("UpdateCenterTest/update-center-dependencies.properties").toString())
      // FxCop is the child of DotNet
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.0", "fxcop-plugin-v10", "1.0"))
      .build();

    try {
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(getServerLog()).contains("The plugin 'dotnet' required by the plugin 'fxcop' is missing");
    }
  }

  @Test
  public void should_not_start_when_parent_version_is_different() throws IOException {
    orchestrator = Orchestrator
      .builderEnv()
      .setServerProperty("sonar.updatecenter.url",
        UpdateCenterDependenciesTest.class.getResource("UpdateCenterTest/update-center-dependencies.properties").toString())
      // FxCop is the child of DotNet
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.1", "fxcop-plugin-v11", "1.1"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.0", "dotnet-plugin-v10", "1.0"))
      .build();

    try {
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(getServerLog()).contains("The plugins 'fxcop' and 'dotnet' must have exactly the same version as they belong to the same group");
    }
  }

  @Test
  public void should_uninstall_children_and_dependencies() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url", getUpdateCenterUrlPath())
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.0", "dotnet-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.0", "fxcop-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/csharp-plugin-1.0", "csharp-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/visualstudio-plugin-1.0", "visualstudio-plugin-v10", "1.0"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("updatecenter-uninstall_children_and_dependencies",
      "/selenium/server/updatecenter/uninstall_parent_with_children.html",
      "/selenium/server/updatecenter/uninstall_dependencies.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_install_new_plugins_with_dependencies_and_children() throws Exception {
    setUpdateCenterProperties();
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url", getUpdateCenterUrlPath())
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("updatecenter-server-update-center-dependencies",
      "/selenium/server/updatecenter/install_new_plugins_with_dependencies_and_children.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_install_new_plugins_with_dependencies_not_already_installed() throws Exception {
    setUpdateCenterProperties();
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url", getUpdateCenterUrlPath())
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.1", "dotnet-plugin-v11", "1.1"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.1", "fxcop-plugin-v11", "1.1"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("updatecenter-install-new_plugins-with-dependencies-not-already-installed",
      "/selenium/server/updatecenter/install_new_plugins_with_dependencies_not_already_installed.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_update_plugins_with_dependencies_and_children() throws Exception {
    setUpdateCenterProperties();
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url", getUpdateCenterUrlPath())
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.0", "dotnet-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.0", "fxcop-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/csharp-plugin-1.0", "csharp-plugin-v10", "1.0"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("updatecenter-update-plugins-with-dependencies-and-children",
      "/selenium/server/updatecenter/update_plugins_with_dependencies_and_children.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_update_plugins_without_incoming_dependencies_not_installed() throws Exception {
    setUpdateCenterProperties();
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url", getUpdateCenterUrlPath())
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.0", "dotnet-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.0", "fxcop-plugin-v10", "1.0"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("updatecenter-update-plugins-without-incoming-dependencies-not-installed",
      "/selenium/server/updatecenter/update_plugins_without_incoming_dependencies_not_installed.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4476
   */
  @Test
  public void should_not_update_plugin_having_dependencies_needed_sonar_upgrade() throws Exception {
    setUpdateCenterProperties();
    // C Sharp 1.1 requires DotNet 1.1
    setUpdateCenterProperty("dotnet.1.1.sqVersions", "10.0");
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url", getUpdateCenterUrlPath())
      .addPlugin(ItUtils.locateTestPlugin("update-center/dotnet-plugin-1.0", "dotnet-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/fxcop-plugin-1.0", "fxcop-plugin-v10", "1.0"))
      .addPlugin(ItUtils.locateTestPlugin("update-center/csharp-plugin-1.0", "csharp-plugin-v10", "1.0"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("updatecenter-not-update-plugin-having-dependencies-needed-sonar-upgrade",
      "/selenium/server/updatecenter/not_update_plugin_having_dependencies_needed_sonar_upgrade.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  private void setUpdateCenterProperties() throws Exception {
    setUpdateCenterProperty("dotnet.1.1.downloadUrl", getUrlPath(temp.newFile("dotnet-plugin-v11-1.1.jar")));
    setUpdateCenterProperty("fxcop.1.1.downloadUrl", getUrlPath(temp.newFile("fxcop-plugin-v11-1.1.jar")));
    setUpdateCenterProperty("csharp.1.1.downloadUrl", getUrlPath(temp.newFile("csharp-plugin-v11-1.1.jar")));
    setUpdateCenterProperty("visualstudio.1.1.downloadUrl", getUrlPath(temp.newFile("visualstudio-plugin-v11-1.1.jar")));
  }

  private void setUpdateCenterProperty(String key, String value) throws Exception {
    FileInputStream in = new FileInputStream(updateCenterFile);
    try {
      Properties props = new Properties();
      props.load(in);
      props.setProperty(key, value);
      props.store(new FileOutputStream(updateCenterFile), "Update center file copied.");

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private String getServerLog() throws IOException {
    File logFile = orchestrator.getServer().getLogs();
    return Files.toString(logFile, Charsets.UTF_8);
  }

  private String getUpdateCenterUrlPath() throws MalformedURLException {
    return getUrlPath(updateCenterFile);
  }

  private String getUrlPath(File file) throws MalformedURLException {
    return file.toURI().toURL().toString();
  }

}
