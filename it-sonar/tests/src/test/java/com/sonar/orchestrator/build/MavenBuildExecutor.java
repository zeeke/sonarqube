/*
 * Copyright (C) 2011-2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.orchestrator.build;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 *TODO This class is used to override the one for Orchestrator in order to workaround ORCH-186
 *
 */
class MavenBuildExecutor extends AbstractBuildExecutor<MavenBuild> {

  private static final String MAVEN_OPTS = "MAVEN_OPTS";
  private static final String MOJO_VERSION_PROP = "orchestrator.mojo.version";

  @VisibleForTesting
  BuildResult execute(MavenBuild build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor commandExecutor) {
    DefaultBuildResult result = new DefaultBuildResult();
    String mojoVersion = config.getString(MOJO_VERSION_PROP);
    for (String goal : build.getGoals()) {
      appendCoverageArgumentToOpts(build.getEnvironmentVariables(), config, MAVEN_OPTS);
      if (StringUtils.isNotBlank(mojoVersion)) {
        goal = goal.replaceAll("sonar:sonar", "org.codehaus.mojo:sonar-maven-plugin:" + mojoVersion.toString() + ":sonar");
      }
      executeGoal(build, config, adjustedProperties, goal, result, commandExecutor);
    }
    return result;
  }

  private void executeGoal(MavenBuild build, Configuration config, Map<String, String> adjustedProperties, String goal,
      final DefaultBuildResult result, CommandExecutor commandExecutor) {
    try {
      Command command = Command.create(getMvnPath(config.fileSystem().mavenHome()));
      if (build.getExecutionDir() != null) {
        command.setDirectory(build.getExecutionDir());
      }
      for (Map.Entry<String, String> env : build.getEnvironmentVariables().entrySet()) {
        command.setEnvironmentVariable(env.getKey(), env.getValue());
      }
      // allow to set "clean install" in the same process
      command.addArguments(StringUtils.split(goal, " "));
      command.addArgument("-B");
      command.addArgument("-e");
      if (build.getPom() != null) {
        File pomFile = config.fileSystem().locate(build.getPom());
        Preconditions.checkState(pomFile.exists(), "Maven pom does not exist: " + build.getPom());
        command.addArgument("-f").addArgument(pomFile.getAbsolutePath());
      }
      if (build.isDebugLogs()) {
        command.addArgument("-X");
      }
      for (Map.Entry entry : adjustedProperties.entrySet()) {
        command.addSystemArgument(entry.getKey().toString(), entry.getValue().toString());
      }
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      LoggerFactory.getLogger(getClass()).info("Execute: " + command);
      int status = commandExecutor.execute(command, writer, writer, build.getTimeoutSeconds() * 1000);
      result.setStatus(status);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute Maven", e);
    }
  }

  static String getMvnPath(File mvnHome) throws IOException {
    String program = "mvn";
    if (SystemUtils.IS_OS_WINDOWS) {
      program += ".bat";
    }
    if (mvnHome != null) {
      program = new File(mvnHome, "bin/" + program).getCanonicalPath();
    }
    return program;
  }

}
