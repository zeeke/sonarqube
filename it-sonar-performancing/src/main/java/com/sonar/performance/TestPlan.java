/*
 * Copyright (C) 2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.performance;

import com.sonar.orchestrator.Orchestrator;

import java.util.List;

class TestPlan {
  private String[] versionsOnExistingDb = new String[0];
  private String[] versionsOnFreshDb = new String[0];
  private List<Task> tasks;

  TestPlan setVersionsOnExistingDb(String... versionsOnExistingDb) {
    this.versionsOnExistingDb = versionsOnExistingDb;
    return this;
  }

  TestPlan setVersionsOnFreshDb(String... versionsOnFreshDb) {
    this.versionsOnFreshDb = versionsOnFreshDb;
    return this;
  }

  TestPlan setTasks(List<Task> tasks) {
    this.tasks = tasks;
    return this;
  }

  void execute() throws Exception {
    Report report = new Report();
    run(report, versionsOnExistingDb, true);
    run(report, versionsOnFreshDb, false);
    report.dump();
  }

  private void run(Report report, String[] versions, boolean keepDatabase) throws Exception {
    for (String version : versions) {
      report.setCurrentVersion(version + (keepDatabase ? " (FULL DB)" : " (EMPTY DB)"));
      Orchestrator orchestrator = Orchestrator.builderEnv()
        .setSonarVersion(version)
        .setOrchestratorProperty("orchestrator.keepDatabase", String.valueOf(keepDatabase))
        .build();
      try {
        run(orchestrator, report);
      } finally {
        orchestrator.stop();
      }

    }
  }

  private void run(Orchestrator orchestrator, Report report) throws Exception {
    for (Task task : tasks) {
      Counters counters = new Counters();
      if (task instanceof PerformanceTask) {
        PerformanceTask perfTask = (PerformanceTask) task;
        System.out.println("\n\n************************* " + report.currentVersion() + " " + perfTask.name() + "\n\n");
        for (int i = 0; i < perfTask.replay(); i++) {
          task.execute(orchestrator, counters);
        }
        report.add(perfTask.name(), counters);
      } else {
        task.execute(orchestrator, counters);
      }
    }
  }
}
