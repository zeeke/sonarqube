/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.JmxUtils;
import org.sonar.process.Monitor;
import org.sonar.process.ProcessMXBean;
import org.sonar.process.ProcessUtils;
import org.sonar.process.ProcessWrapper;

public class App implements ProcessMXBean {

  private final Installation installation;

  private final Monitor monitor = new Monitor();
  private ProcessWrapper elasticsearch;
  private ProcessWrapper server;

  public App(Installation installation) throws Exception {
    this.installation = installation;
    JmxUtils.registerMBean(this, "SonarQube");
    ProcessUtils.addSelfShutdownHook(this);
  }

  public void start() throws InterruptedException {
    try {
      Logger logger = LoggerFactory.getLogger(getClass());
      monitor.start();

      elasticsearch = new ProcessWrapper(JmxUtils.SEARCH_SERVER_NAME)
        .setWorkDir(installation.homeDir())
        .setJmxPort(Integer.parseInt(installation.prop(DefaultSettings.ES_JMX_PORT_KEY)))
        .addJavaOpts(installation.prop(DefaultSettings.ES_JAVA_OPTS_KEY))
        .addJavaOpts(String.format("-Djava.io.tmpdir=%s", installation.tempDir().getAbsolutePath()))
        .addJavaOpts(String.format("-Dsonar.path.logs=%s", installation.logsDir().getAbsolutePath()))
        .setClassName("org.sonar.search.SearchServer")
        .setProperties(installation.props().encryptedProperties())
        .addClasspath(installation.starPath("lib/common"))
        .addClasspath(installation.starPath("lib/search"));
      if (elasticsearch.execute()) {
        monitor.registerProcess(elasticsearch);
        if (elasticsearch.waitForReady()) {
          logger.info("Search server is ready");

          server = new ProcessWrapper(JmxUtils.WEB_SERVER_NAME)
            .setWorkDir(installation.homeDir())
            .setJmxPort(Integer.parseInt(installation.prop(DefaultSettings.WEB_JMX_PORT_KEY)))
            .addJavaOpts(installation.prop(DefaultSettings.WEB_JAVA_OPTS_KEY))
            .addJavaOpts(DefaultSettings.WEB_JAVA_OPTS_APPENDED_VAL)
            .addJavaOpts(String.format("-Djava.io.tmpdir=%s", installation.tempDir().getAbsolutePath()))
            .addJavaOpts(String.format("-Dsonar.path.logs=%s", installation.logsDir().getAbsolutePath()))
            .setClassName("org.sonar.server.app.WebServer")
            .setProperties(installation.props().encryptedProperties())
            .addClasspath(installation.starPath("extensions/jdbc-driver/mysql"))
            .addClasspath(installation.starPath("extensions/jdbc-driver/mssql"))
            .addClasspath(installation.starPath("extensions/jdbc-driver/oracle"))
            .addClasspath(installation.starPath("extensions/jdbc-driver/postgresql"))
            .addClasspath(installation.starPath("lib/common"))
            .addClasspath(installation.starPath("lib/server"));
          if (server.execute()) {
            monitor.registerProcess(server);
            if (server.waitForReady()) {
              logger.info("Web server is ready");
              monitor.join();
            }
          }
        }
      }
    } finally {
      terminate();
    }
  }

  @Override
  public boolean isReady() {
    return monitor.isAlive();
  }

  @Override
  public long ping() {
    return System.currentTimeMillis();
  }

  @Override
  public void terminate() {
    LoggerFactory.getLogger(App.class).info("Stopping");
    if (monitor.isAlive()) {
      monitor.terminate();
      monitor.interrupt();
    }
    if (server != null) {
      server.terminate();
    }
    if (elasticsearch != null) {
      elasticsearch.terminate();
    }
  }

  public static void main(String[] args) throws Exception {
    Installation installation = new Installation();
    new AppLogging().configure(installation);
    new App(installation).start();
  }
}