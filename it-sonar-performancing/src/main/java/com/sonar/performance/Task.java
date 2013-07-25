/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.orchestrator.Orchestrator;

public interface Task {
  void execute(Orchestrator orchestrator, Counters counters) throws Exception;
}
